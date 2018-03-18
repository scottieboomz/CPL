package com.google.android.apps.nexuslauncher;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.TwoStatePreference;
import android.text.TextUtils;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.IconCache;
import com.android.launcher3.IconsHandler;
import com.android.launcher3.MultiSelectRecyclerViewActivity;

import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.graphics.IconShapeOverride;
import com.google.android.apps.nexuslauncher.smartspace.SmartspaceController;

public class SettingsActivity extends com.android.launcher3.SettingsActivity implements PreferenceFragment.OnPreferenceStartFragmentCallback {
    public final static String SHOW_PREDICTIONS_PREF = "pref_show_predictions";
    public final static String ENABLE_MINUS_ONE_PREF = "pref_enable_minus_one";
    public final static String SMARTSPACE_PREF = "pref_smartspace";
    public final static String APP_VERSION_PREF = "about_app_version";
    public final static String APP_BUILD_DATE_PREF = "about_app_build_date";
    public final static String ICON_PACK_PREF = "icon-packs";


    private String mDefaultIconPack;
    private IconsHandler mIconsHandler;
    private PackageManager mPackageManager;
    private Preference mIconPack;

    @Override
    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        if (bundle == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content, new MySettingsFragment()).commit();
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment, Preference preference) {
        Fragment instantiate = Fragment.instantiate(this, preference.getFragment(), preference.getExtras());
        if (instantiate instanceof DialogFragment) {
            ((DialogFragment) instantiate).show(getFragmentManager(), preference.getKey());
        } else {
            getFragmentManager().beginTransaction().replace(android.R.id.content, instantiate).addToBackStack(preference.getKey()).commit();
        }
        return true;
    }

    public static class MySettingsFragment extends com.android.launcher3.SettingsActivity.LauncherSettingsFragment
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
        private Context mContext;

        private String mDefaultIconPack;
        private IconsHandler mIconsHandler;
        private PackageManager mPackageManager;
        private Preference mIconPack;

        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);

            mContext = getActivity();

            findPreference(SHOW_PREDICTIONS_PREF).setOnPreferenceChangeListener(this);
            findPreference(ENABLE_MINUS_ONE_PREF).setTitle(getDisplayGoogleTitle());

            PackageManager packageManager = mContext.getPackageManager();
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
                findPreference(APP_VERSION_PREF).setSummary(packageInfo.versionName);
                if (SmartspaceController.get(mContext).cY()) {
                    findPreference(SMARTSPACE_PREF).setOnPreferenceClickListener(this);
                } else {
                    getPreferenceScreen().removePreference(findPreference("pref_smartspace"));
                }
            } catch (PackageManager.NameNotFoundException ex) {
                Log.e("SettingsActivity", "Unable to load my own package info", ex);
            }
            findPreference(APP_BUILD_DATE_PREF).setSummary(BuildConfig.BUILD_TIME + "\n" + "(" + BuildConfig.BUILD_HOST + "/" + BuildConfig.BUILD_WHOAMI + ")");

            Preference vibrate = findPreference(Utilities.VIBRATIONFEEDBACKTEST);
            vibrate.setOnPreferenceClickListener(this);

            Preference hiddenApp = findPreference(Utilities.KEY_HIDDEN_APPS);
            hiddenApp.setOnPreferenceClickListener(this);

            Preference reboot = findPreference(Utilities.KEY_REBOOT);
            reboot.setOnPreferenceClickListener(this);

            Preference forgot = findPreference(Utilities.KEY_ABOUT_FORGOT);
            forgot.setOnPreferenceClickListener(this);

            Preference pack = findPreference(Utilities.KEY_ICON_PACK);
            pack.setOnPreferenceClickListener(this);

            findPreference(SHOW_PREDICTIONS_PREF).setOnPreferenceChangeListener(this);


            if (Utilities.ATLEAST_NOUGAT) {
                getPreferenceScreen().findPreference("pref_DateFormats").setEnabled(true);
                getPreferenceScreen().findPreference("pref_allappqsb_color_picker").setEnabled(true);
            }
            if (Utilities.ATLEAST_OREO) {
                getPreferenceScreen().findPreference("pref_override_icon_shape").setEnabled(true);
            }
            if (Utilities.ATLEAST_MARSHMALLOW) {
                getPreferenceScreen().findPreference("pref_icon_badging").setEnabled(true);
            }

            mPackageManager = getActivity().getPackageManager();
            mDefaultIconPack = getString(R.string.default_iconpack);
            mIconsHandler = IconCache.getIconsHandler(getActivity().getApplicationContext());
            mIconPack = (Preference) findPreference(Utilities.KEY_ICON_PACK);
            reloadIconPackSummary();
        }

        private String getDisplayGoogleTitle() {
            CharSequence charSequence = null;
            try {
                Resources resourcesForApplication = mContext.getPackageManager().getResourcesForApplication("com.google.android.googlequicksearchbox");
                int identifier = resourcesForApplication.getIdentifier("title_google_home_screen", "string", "com.google.android.googlequicksearchbox");
                if (identifier != 0) {
                    charSequence = resourcesForApplication.getString(identifier);
                }
            }
            catch (PackageManager.NameNotFoundException ex) {
            }
            if (TextUtils.isEmpty(charSequence)) {
                charSequence = mContext.getString(R.string.title_google_app);
            }
            return mContext.getString(R.string.title_show_google_app, charSequence);
        }

        @Override
        public void onResume() {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
            super.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
            PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
            mIconsHandler.hideDialog();
        }


       public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
             reloadIconPackSummary();
        }



        @Override
        public boolean onPreferenceChange(Preference preference, final Object newValue) {
            switch (preference.getKey()) {
                case SHOW_PREDICTIONS_PREF:
                    if ((boolean) newValue) {
                        return true;
                    }
                    SettingsActivity.SuggestionConfirmationFragment confirmationFragment = new SettingsActivity.SuggestionConfirmationFragment();
                    confirmationFragment.setTargetFragment(this, 0);
                    confirmationFragment.show(getFragmentManager(), preference.getKey());
                    break;
            }
            return false;
        }

        private void reloadIconPackSummary() {
            Preference preference = findPreference(ICON_PACK_PREF);
            if (preference == null) {
                return;
            }

            String defaultPack = mContext.getString(R.string.default_iconpack);
            String iconPack = PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString(Utilities.KEY_ICON_PACK, defaultPack);
            Drawable icon = getResources().getDrawable(android.R.mipmap.sym_def_app_icon);
            try {
                ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(iconPack, 0);
                preference.setSummary(mContext.getPackageManager().getApplicationLabel(info));
                icon = mPackageManager.getApplicationIcon(info);
                preference.setIcon(icon);
            } catch (PackageManager.NameNotFoundException e) {
                icon = getResources().getDrawable(R.mipmap.ic_cpl_bowie_lightning_bolt);
                preference.setSummary(defaultPack);
                preference.setIcon(icon);
            }
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (SMARTSPACE_PREF.equals(preference.getKey())) {
                SmartspaceController.get(mContext).cZ();
                return true;
            }

            if (Utilities.KEY_HIDDEN_APPS.equals(preference.getKey())) {
                Intent intent = new Intent(getActivity(), MultiSelectRecyclerViewActivity.class);
                startActivity(intent);
                return true;
            }
            if (Utilities.KEY_ICON_PACK.equals(preference.getKey())) {
                mIconsHandler.showDialog(getActivity());
                return true;
            }
            if (Utilities.KEY_REBOOT.equals(preference.getKey())) {
                android.os.Process.killProcess(android.os.Process.myPid());
                return true;
            }
            if (Utilities.VIBRATIONFEEDBACKTEST.equals(preference.getKey())) {
                Vibrator r = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                r.vibrate(Integer.valueOf(Utilities.getPrefs(mContext).getString("pref_vibrationduration", "50")));
                return true;
            }
            if (Utilities.KEY_ABOUT_FORGOT.equals(preference.getKey())) {
                SharedPreferences devicePrefs = Utilities.getPrefs(mContext);
                devicePrefs.edit().putInt("key_ee", devicePrefs.getInt("key_ee", 0) + 1).apply();
                int i = devicePrefs.getInt("key_ee", 0);
                if (i <= 5) {
                    Toast toast = Toast.makeText(mContext,R.string.forgot,Toast.LENGTH_LONG);
                    toast.show();
                }
                if (i >= 5 && i <= 14) {
                    Toast toast = Toast.makeText(mContext,String.valueOf(i) + "/15",Toast.LENGTH_SHORT);
                    toast.show();
                }
                if (i == 15) {
                    Toast toast = Toast.makeText(mContext,R.string.ee,Toast.LENGTH_LONG);
                    devicePrefs.edit().putInt("key_ee", 0).apply();
                    toast.show();
                }
                return true;
            }
            return false;
        }
    }




    public static class SuggestionConfirmationFragment extends DialogFragment implements DialogInterface.OnClickListener {
        public void onClick(final DialogInterface dialogInterface, final int n) {
            if (getTargetFragment() instanceof PreferenceFragment) {
                Preference preference = ((PreferenceFragment) getTargetFragment()).findPreference(SHOW_PREDICTIONS_PREF);
                if (preference instanceof TwoStatePreference) {
                    ((TwoStatePreference) preference).setChecked(false);
                }
            }
        }

        public Dialog onCreateDialog(final Bundle bundle) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.title_disable_suggestions_prompt)
                    .setMessage(R.string.msg_disable_suggestions_prompt)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.label_turn_off_suggestions, this).create();
        }
    }
}
