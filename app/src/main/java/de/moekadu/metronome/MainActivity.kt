/*
 * Copyright 2019 Michael Moessner
 *
 * This file is part of Metronome.
 *
 * Metronome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metronome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Metronome.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.moekadu.metronome

import android.annotation.SuppressLint
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    // TODO: handle incorrect loads which could occur, when loading with newer version
    // TODO: nicer saved-item layout
    // TODO: double/half speed?
    // TODO: delete log messages?

    // TODO: when volume sliders are shown and a note is added/removed, animate the sliders to fit then new note setup
    // TODO: test different device formats
    // TODO: static soundchooser layout in landscape could be improved

    companion object {
        private const val METRONOME_FRAGMENT_TAG = "metronomeFragment"
        private const val PLAYER_FRAGMENT_TAG = "playerFragment"
        private const val SETTINGS_FRAGMENT_TAG = "settingsFragment"
//        private const val SOUND_CHOOSER_FRAGMENT_TAG = "soundChooserDialog"
        private const val SAVE_DATA_FRAGMENT_TAG = "saveDataFragment"
    }

    private var playerFrag : PlayerFragment? = null
    private var settingsFrag : SettingsFragment? = null
    // private var soundChooserDialog : SoundChooserDialog? = null
    private var saveDataFragment : SaveDataFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        Log.v("Metronome", "MainActivity:onCreate")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val appearance = sharedPreferences.getString("appearance", "auto")
        var nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

        if(appearance.equals("dark")){
            nightMode = AppCompatDelegate.MODE_NIGHT_YES
        }
        else if(appearance.equals("light")){
            nightMode = AppCompatDelegate.MODE_NIGHT_NO
        }

        AppCompatDelegate.setDefaultNightMode(nightMode)

        val screenOn = sharedPreferences.getBoolean("screenon", false)
        if(screenOn)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        volumeControlStream = AudioManager.STREAM_MUSIC

        playerFrag = supportFragmentManager.findFragmentByTag(PLAYER_FRAGMENT_TAG) as PlayerFragment?
        if(playerFrag == null) {
            playerFrag = PlayerFragment()
            playerFrag?.let {
                supportFragmentManager.beginTransaction().add(it, PLAYER_FRAGMENT_TAG).commit()
            }
        }

        var metrFrag = supportFragmentManager.findFragmentByTag(METRONOME_FRAGMENT_TAG) as MetronomeFragment?
        if(metrFrag == null) {
            metrFrag = MetronomeFragment()
        }

        settingsFrag = supportFragmentManager.findFragmentByTag(SETTINGS_FRAGMENT_TAG) as SettingsFragment?
        if(settingsFrag == null) {
            settingsFrag = SettingsFragment()
        }

//        soundChooserDialog = supportFragmentManager.findFragmentByTag(SOUND_CHOOSER_FRAGMENT_TAG) as SoundChooserDialog?
//        if(soundChooserDialog == null) {
//            soundChooserDialog = SoundChooserDialog()
//            soundChooserDialog?.let {
//                supportFragmentManager.beginTransaction()
//                        .add(R.id.dialogframe, it, SOUND_CHOOSER_FRAGMENT_TAG)
//                        .detach(it)
//                        .commit()
//            }
//        }
//        soundChooserDialog?.setOnBackgroundClickedListener { unloadSoundChooserDialog() }

        saveDataFragment = supportFragmentManager.findFragmentByTag(SAVE_DATA_FRAGMENT_TAG) as SaveDataFragment?
        if(saveDataFragment == null) {
            saveDataFragment = SaveDataFragment()
        }
        saveDataFragment?.onItemClickedListener = object : SavedItemDatabase.OnItemClickedListener {
            override fun onItemClicked(item: SavedItemDatabase.SavedItem, position: Int) {
                loadSettings(item)
                supportFragmentManager.popBackStack()
            }
        }

        if(supportFragmentManager.fragments.size == 0)
            supportFragmentManager.beginTransaction().replace(R.id.mainframe, metrFrag, METRONOME_FRAGMENT_TAG).commit()

        setDisplayHomeButton()
        supportFragmentManager.addOnBackStackChangedListener { setDisplayHomeButton() }
//        Log.v("Metronome", "MainActivity:onCreate: end");
    }


//    override fun onPause() {
////        unloadSoundChooserDialog()
//        super.onPause()
//    }

    override fun onSupportNavigateUp() : Boolean{
        supportFragmentManager.popBackStack()
        return true
    }

    private fun setDisplayHomeButton() {
        val showDisplayHomeButton = supportFragmentManager.backStackEntryCount > 0
        supportActionBar?.setDisplayHomeAsUpEnabled(showDisplayHomeButton)
    }

    override fun onCreateOptionsMenu(menu : Menu) : Boolean{
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item : MenuItem) : Boolean{
        //    // Handle action bar item clicks here. The action bar will
        //    // automatically handle clicks on the Home/Up button, so long
        //    // as you specify a parent activity in AndroidManifest.xml.
        //    //noinspection SimplifiableIfStatement
        when (item.itemId) {
            R.id.action_properties -> {
                settingsFrag?.let {
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.mainframe, it, SETTINGS_FRAGMENT_TAG)
                            .addToBackStack("blub")
                            .commit()
                }
            }
            R.id.action_load -> {
                saveDataFragment?.let {
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.mainframe, it, SAVE_DATA_FRAGMENT_TAG)
                            .addToBackStack("blub")
                            .commit()
                }
            }
            R.id.action_save -> {
                saveCurrentSettings()
            }
//        else if(id == R.id.test_setting) {
//            fragManager.beginTransaction()
//                    .attach(soundChooserDialog)
//                    //.replace(R.id.dialogframe, soundChooserDialog, soundChooserDialogNewTag)
//                    .commit();
//        }
        }
//        else if(id == R.id.test_setting) {
//            fragManager.beginTransaction()
//                    .attach(soundChooserDialog)
//                    //.replace(R.id.dialogframe, soundChooserDialog, soundChooserDialogNewTag)
//                    .commit();
//        }

        return super.onOptionsItemSelected(item)
    }

//    fun loadSoundChooserDialog(button : MoveableButton, playerService : PlayerService) {
//        soundChooserDialog?.let {
//            it.setStatus(button, playerService)
//            supportFragmentManager.beginTransaction()
//                    .attach(it)
//                    .commit()
//        }
//    }
//
//    private fun unloadSoundChooserDialog(){
//        soundChooserDialog?.let {
//            supportFragmentManager.beginTransaction()
//                    .detach(it)
//                    .commit()
//        }
//    }

    @SuppressLint("SimpleDateFormat")
    private fun saveCurrentSettings() {
//        Log.v("Metronome", "MainActivity:saveCurrentSettings");
        val editText = EditText(this)
//        editText.setPadding(dp_to_px(8), dp_to_px(8), dp_to_px(8), dp_to_px(8));
        editText.setHint(R.string.save_name)
        val dialogBuilder = AlertDialog.Builder(this)
        editText.inputType = InputType.TYPE_CLASS_TEXT
        dialogBuilder
                .setTitle(R.string.save_settings_dialog_title)
                .setView(editText)
                .setPositiveButton(R.string.save
                ) { _, _ ->
                    val item = SavedItemDatabase.SavedItem()
                    item.title = editText.text.toString()
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy")
                    val timeFormat = SimpleDateFormat("hh:mm")
                    val date = Calendar.getInstance().time
                    item.date = dateFormat.format(date)
                    item.time = timeFormat.format(date)

                    item.bpm = playerFrag?.playerService?.speed ?: InitialValues.speed
                    //item.noteList = playerFrag?.playerService?.metaData?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: ""
                    item.noteList = SoundProperties.createMetaDataString(playerFrag?.playerService?.noteList)
                    //                    Log.v("Metronome", item.playList);
                    if (item.title.length > 200) {
                        item.title = item.title.substring(0, 200)
                        Toast.makeText(this@MainActivity, getString(R.string.max_allowed_characters, 200), Toast.LENGTH_SHORT).show()
                    }
                    saveDataFragment?.saveItem(this@MainActivity, item)
                    Toast.makeText(this@MainActivity, getString(R.string.saved_item_message, item.title), Toast.LENGTH_SHORT).show()
                }.setNegativeButton(R.string.dismiss) { dialog, _ -> dialog.cancel() }
        dialogBuilder.show()
    }

    private fun loadSettings(item : SavedItemDatabase.SavedItem) {
//        Log.v("Metronome", "MainActivity:loadSettings");
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val minimumSpeedString = sharedPreferences.getString("minimumspeed", InitialValues.minimumSpeed.toString()) ?: InitialValues.minimumSpeed.toString()
        val minimumSpeed = minimumSpeedString.toFloat()
        val maximumSpeedString = sharedPreferences.getString("maximumspeed", InitialValues.maximumSpeed.toString()) ?: InitialValues.maximumSpeed.toString()
        val maximumSpeed = maximumSpeedString.toFloat()
        val speedIncrementIndex = sharedPreferences.getInt("speedincrement", InitialValues.speedIncrementIndex)
        val speedIncrement = Utilities.speedIncrements[speedIncrementIndex]
        val tolerance = 1.0e-6f
        val stringBuilder = StringBuilder()
        if(item.bpm < minimumSpeed - tolerance)
            stringBuilder.append(getString(R.string.speed_too_small, Utilities.getBpmString(item.bpm), Utilities.getBpmString(minimumSpeed)))
        if(item.bpm > maximumSpeed + tolerance)
            stringBuilder.append(getString(R.string.speed_too_large, Utilities.getBpmString(item.bpm), Utilities.getBpmString(maximumSpeed)))
        if(abs(item.bpm /  speedIncrement - (item.bpm / speedIncrement).roundToInt()) > tolerance)
            stringBuilder.append(getString(R.string.inconsistent_increment, Utilities.getBpmString(item.bpm), Utilities.getBpmString(speedIncrement)))
        if(stringBuilder.isNotEmpty()) {
            stringBuilder.append(getString(R.string.inconsistent_summary))
            val builder = AlertDialog.Builder(this)
                    .setTitle(R.string.inconsistent_load_title)
                    .setMessage(stringBuilder.toString())
                    .setNegativeButton(R.string.acknowledged) { dialog, _ -> dialog.dismiss() }
            builder.show()
        }

        playerFrag?.playerService?.speed = item.bpm
        playerFrag?.playerService?.noteList = SoundProperties.parseMetaDataString(item.noteList)
        Toast.makeText(this, getString(R.string.loaded_message, item.title), Toast.LENGTH_SHORT).show()
    }
}
