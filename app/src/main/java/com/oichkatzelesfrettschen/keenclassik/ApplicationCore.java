/*
 * ApplicationCore.java: Application-level initialization and preferences
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import com.oichkatzelesfrettschen.keenclassik.data.FlavorConfig;
import com.oichkatzelesfrettschen.keenclassik.data.FlavorConfigProvider;
import com.oichkatzelesfrettschen.keenclassik.data.KeenProfile;

import static com.oichkatzelesfrettschen.keenclassik.MenuActivity.DARK_MODE;
import static com.oichkatzelesfrettschen.keenclassik.MenuActivity.MENU_DIFF;
import static com.oichkatzelesfrettschen.keenclassik.MenuActivity.MENU_MULT;
import static com.oichkatzelesfrettschen.keenclassik.MenuActivity.MENU_PROFILE;
import static com.oichkatzelesfrettschen.keenclassik.MenuActivity.MENU_SIZE;

public class ApplicationCore extends Application {

    private boolean isDarkMode;
    private int gameSize=3;
    private int gameDiff=1;
    private int gameMult=0;
    private boolean canCont=false;
    private KeenProfile gameProfile = KeenProfile.DEFAULT;

    private SharedPreferences sharedPref;

    public boolean isDarkMode() {
        return isDarkMode;
    }

    public int getGameSize() {
        return gameSize;
    }

    public void setGameSize(int gameSize) {
        this.gameSize = gameSize;
    }

    public int getGameDiff() {
        return gameDiff;
    }

    public void setGameDiff(int gameDiff) {
        this.gameDiff = gameDiff;
    }

    public int getGameMult() {
        return gameMult;
    }

    public void setGameMult(int gameMult) {
        this.gameMult = gameMult;
    }

    public KeenProfile getGameProfile() {
        return gameProfile;
    }

    public void setGameProfile(KeenProfile gameProfile) {
        this.gameProfile = gameProfile;
    }

    public boolean isCanCont() {
        return canCont;
    }

    public void setCanCont(boolean canCont) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(KeenActivity.CAN_CONT,canCont);
        editor.apply();
        this.canCont = canCont;
    }

    public void savePrefs() {

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(MENU_MULT,gameMult!=0);
        editor.putInt(MENU_DIFF,gameDiff);
        editor.putInt(MENU_SIZE,gameSize);
        editor.putString(MENU_PROFILE, gameProfile.name());
        editor.putBoolean(DARK_MODE, isDarkMode);
        editor.apply();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        FlavorConfigProvider.set(new FlavorConfig() {
            @Override
            public boolean getFullModeSet() {
                return BuildConfig.ADVANCED_MODES_ENABLED;
            }

            @Override
            public int getMinGridSize() {
                return BuildConfig.MIN_GRID_SIZE;
            }

            @Override
            public int getMaxGridSize() {
                return BuildConfig.MAX_GRID_SIZE;
            }
        });

        sharedPref = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        TestEnvironment.primeFromContext(this);

        canCont= sharedPref.getBoolean(KeenActivity.CAN_CONT,false);

        gameDiff = sharedPref.getInt(MENU_DIFF,0);

        gameSize = sharedPref.getInt(MENU_SIZE,3);

        isDarkMode = sharedPref.getBoolean(DARK_MODE, false);

        gameMult = sharedPref.getBoolean(MENU_MULT,false) ? 1 : 0;

        String profileName = sharedPref.getString(MENU_PROFILE, KeenProfile.DEFAULT.name());
        gameProfile = KeenProfile.fromName(profileName);
    }

}
