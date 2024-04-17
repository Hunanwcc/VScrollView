package cn.wcc.scrollview

import android.bluetooth.BluetoothProfile
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import cn.wcc.customScroll.VScrollView
import cn.wcc.scrollview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var mBinding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)

        mBinding = DataBindingUtil.bind(layoutInflater.inflate(R.layout.activity_main,null))
        setContentView(mBinding?.root)

        val devicec: BluetoothProfile

        mBinding?.vsc?.setDataCallback(object: VScrollView.OnStateChangedCallback{
            override fun onItemClick(index: Int, type: VScrollView.ClickItemType?) {
                Log.e("MainActivity", "onItemClick: $index type:$type")
            }

            override fun viewDisableClicked() {
                Log.e("MainActivity", "viewDisableClicked: ")
            }

        } )

//        mBinding?.vsc?.setDataCallback{ index, itemType ->
//            Log.e("MainActivity", "onCreate: $index type:$itemType")
//        }
//        mBinding?.vsc?.setEnableCallback {
//            Log.e("MainActivity", "onCreate: --setEnableCallback---", )
//        }

        mBinding?.vsc?.setViewEnable(false)
    }
}