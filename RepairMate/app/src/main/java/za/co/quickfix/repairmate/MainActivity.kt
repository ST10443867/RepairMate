package za.co.quickfix.repairmate

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import za.co.quickfix.repairmate.ui.RepairMateRoot

/** AppCompatActivity (not ComponentActivity) so AppCompatDelegate can switch the app language. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as RepairMateApp).container
        setContent { RepairMateRoot(container) }
    }
}
