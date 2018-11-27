package com.apptreesoftware.barcodescan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView
import java.lang.ref.WeakReference


class QrHandler(activity: BarcodeScannerActivity) : Handler() {
    private var mActivity: WeakReference<BarcodeScannerActivity>? = null;

    init {
        mActivity = WeakReference<BarcodeScannerActivity>(activity)
    }

    override fun handleMessage(msg: Message?) {
        if (msg?.what == BarcodeScannerActivity.PARSE_QR_OK) {
            mActivity?.get()?.handleResult(msg.data.getString("qrCode"))
        } else if (msg?.what == BarcodeScannerActivity.PARSE_QR_NONE) {
            mActivity?.get()?.scannerView?.startCamera()
        }
        BarcodeScannerActivity.HANDLER = null
        super.handleMessage(msg)
    }
}

class BarcodeScannerActivity : Activity(), ZXingScannerView.ResultHandler {

    lateinit var scannerView: me.dm7.barcodescanner.zxing.ZXingScannerView

    companion object {
        val REQUEST_TAKE_PHOTO_CAMERA_PERMISSION = 100
        val TOGGLE_FLASH = 200

        const val IMAGE_CHOOSER = 300

        const val REQUEST_IMAGE = 1000
        const val REQUEST_STORAGE_PERMISSION = 2000

        const val PARSE_QR_OK = 100
        const val PARSE_QR_NONE = 200

        var HANDLER: Handler? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // add back button
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeButtonEnabled(true)
        title = ""
        scannerView = ZXingScannerView(this)
        scannerView.setAutoFocus(true)
        setContentView(scannerView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        var chooser = menu.add(0, IMAGE_CHOOSER, 0, "")
        chooser.setIcon(android.R.drawable.ic_menu_gallery)
        chooser.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        if (scannerView.flash) {
            val item = menu.add(0,
                    TOGGLE_FLASH, 0, "")
            item.setIcon(android.R.drawable.ic_lock_power_off)
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        } else {
            val item = menu.add(0,
                    TOGGLE_FLASH, 0, "")
            item.setIcon(android.R.drawable.ic_lock_power_off)
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when {
            item.itemId == TOGGLE_FLASH -> {
                scannerView.flash = !scannerView.flash
                this.invalidateOptionsMenu()
                return true

            }
            item.itemId == IMAGE_CHOOSER -> {
                if (HANDLER != null) {
                    return true
                }
                val array = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, array,
                            REQUEST_STORAGE_PERMISSION)
                    return true
                }
                scannerView.stopCamera()
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(intent, REQUEST_IMAGE)
                return true

            }
            item.itemId == android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initHandler() {
        if (HANDLER == null) {
            HANDLER = QrHandler(this)
        }
    }

    override fun onDestroy() {
        HANDLER = null
        super.onDestroy()
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE) {
                initHandler()
                val path = FileUtils().getPathFromUri(this, data?.data)
                Thread(Runnable {
                    val msg = Message();
                    val result = FileUtils.analyzeBitmap(path)
                    if (result != null) {
                        msg.what = PARSE_QR_OK
                        val bundle = Bundle()
                        bundle.putString("qrCode", result.toString())
                        msg.data = bundle
                        HANDLER?.sendMessage(msg)
                    } else {
                        msg.what = PARSE_QR_NONE
                        HANDLER?.sendMessage(msg)
                    }
                }).start()
                return
            }
        }
    }

    override fun onResume() {
        super.onResume()
        scannerView.setResultHandler(this)
        // start camera immediately if permission is already given
        if (!requestCameraAccessIfNecessary()) {
            scannerView.startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        scannerView.stopCamera()
    }

    override fun handleResult(result: Result?) {
        val intent = Intent()
        intent.putExtra("SCAN_RESULT", result.toString())
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    fun handleResult(result: String?) {
        val intent = Intent()
        intent.putExtra("SCAN_RESULT", result)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    fun finishWithError(errorCode: String) {
        val intent = Intent()
        intent.putExtra("ERROR_CODE", errorCode)
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }

    private fun requestCameraAccessIfNecessary(): Boolean {
        val array = arrayOf(Manifest.permission.CAMERA)
        if (ContextCompat
                .checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, array,
                    REQUEST_TAKE_PHOTO_CAMERA_PERMISSION)
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,grantResults: IntArray) {
        when (requestCode) {
            REQUEST_TAKE_PHOTO_CAMERA_PERMISSION -> {
                if (PermissionUtil.verifyPermissions(grantResults)) {
                    scannerView.startCamera()
                } else {
                    finishWithError("PERMISSION_NOT_GRANTED")
                }
            }
            REQUEST_STORAGE_PERMISSION -> {
                if (PermissionUtil.verifyPermissions(grantResults)) {
                    scannerView.stopCamera();
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "image/*"
                    startActivityForResult(intent, REQUEST_IMAGE)
                } else {
                    scannerView.startCamera()
                    Toast.makeText(this, BarcodeScanPlugin.METHOD_CALL?.argument<String>("storageDenied") ?: "storage permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }
}

object PermissionUtil {

    /**
     * Check that all given permissions have been granted by verifying that each entry in the
     * given array is of the value [PackageManager.PERMISSION_GRANTED].

     * @see Activity.onRequestPermissionsResult
     */
    fun verifyPermissions(grantResults: IntArray): Boolean {
        // At least one result must be checked.
        if (grantResults.size < 1) {
            return false
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}
