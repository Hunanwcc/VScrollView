package cn.wcc.scrollview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import cn.wcc.scrollview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var mBinding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)

        mBinding = DataBindingUtil.bind(layoutInflater.inflate(R.layout.activity_main,null))
        setContentView(mBinding?.root)


        mBinding?.vsc?.setDataCallback { index, itemType ->
            Log.e("MainActivity", "onCreate: $index type:$itemType")
        }
        mBinding?.vsc?.setEnableCallback {
            Log.e("MainActivity", "onCreate: --setEnableCallback---", )
        }

        mBinding?.vsc?.setViewEnable(true)
    }
}