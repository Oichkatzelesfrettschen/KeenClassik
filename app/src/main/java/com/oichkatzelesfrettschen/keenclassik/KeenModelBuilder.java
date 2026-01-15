/*
 * KeenModelBuilder.java: JNI bridge for puzzle generation
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2016 Sergey
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import com.oichkatzelesfrettschen.keenclassik.data.KeenProfile;

/**
 * Builder class that bridges Java and Native C (JNI) for Keen puzzle generation.
 * It handles fetching the puzzle definition string from the native layer
 * and parsing it into a KeenModel.
 *
 * JNI Response Format (v2):
 *   Success: "OK:payload_data"
 *   Error:   "ERR:code:message"
 *   Legacy:  "payload_data" (no prefix, for backward compatibility)
 */
public class KeenModelBuilder {

    private static final String PREFIX_OK = "OK:";
    private static final String PREFIX_ERR = "ERR:";

    // Last error message from JNI for debugging/logging
    private String lastJniError = null;

    /**
     * Get the last JNI error message, if any.
     * Useful for debugging when build() returns null.
     */
    public String getLastJniError() {
        return lastJniError;
    }

    /**
     * Parse structured JNI response.
     * @return Payload string on success, null on error
     */
    private String parseJniResponse(String response) {
        if (response == null) {
            lastJniError = "JNI returned null";
            return null;
        }

        if (response.startsWith(PREFIX_ERR)) {
            // Parse error format: "ERR:code:message"
            lastJniError = response.substring(PREFIX_ERR.length());
            Log.w("JNI", "Native error: " + lastJniError);
            return null;
        }

        if (response.startsWith(PREFIX_OK)) {
            // Success format: "OK:payload"
            lastJniError = null;
            return response.substring(PREFIX_OK.length());
        }

        // Legacy format (no prefix) - treat as success
        lastJniError = null;
        return response;
    }

    //instead of rewriting the library in java this uses an NDK to access a modified version
    //of the library in C and then creates a KeenModel based off of it.
    public KeenModel build(int size, int diff, int multOnlt, long seed) {
        return build(size, diff, multOnlt, seed, 0, KeenProfile.DEFAULT.getNativeId()); // Default: MODE_STANDARD
    }

    /**
     * Build with explicit mode flags for Classik.
     * @param modeFlags Bit flags from GameMode.cFlags (Classik uses MODE_STANDARD only)
     */
    public KeenModel build(
        int size,
        int diff,
        int multOnlt,
        long seed,
        int modeFlags,
        int profileId
    )
    {
        String levelAsString = null;

        String rawResponse = getLevelFromC(size, diff, multOnlt, seed, modeFlags, profileId);
        levelAsString = parseJniResponse(rawResponse);

        if (levelAsString == null) {
            Log.e("GEN", "Native generation failed: " + lastJniError);
            return null;
        }

        String ZoneData = "";

        Log.d("GEN",levelAsString);

        KeenModel.GridCell[][] cells = new KeenModel.GridCell[size][size];
        HashSet<Integer> diffZones = new HashSet<>();

        for(int i = 0; i < levelAsString.length(); i+=3)
        {
            int dsfCount = Integer.parseInt(levelAsString.substring(i,i+2));
            diffZones.add(dsfCount);

            if(levelAsString.charAt(i+2)==';')
            {

                ZoneData = levelAsString.substring(0,i+3);
                levelAsString = levelAsString.substring(i+3);
                break;
            }
        }

        int zoneCount = diffZones.size();
        diffZones.clear();

        KeenModel.Zone[] zones = new KeenModel.Zone[zoneCount];
        long[] zoneClues = new long[zoneCount];

        for(int i = 0; i<zoneCount; i++)
        {
            char sym = levelAsString.charAt(i*7);
            int val = Integer.parseInt(levelAsString.substring(i*7+1,i*7+6));
            switch(sym)
            {
                case 'a':
                    zones[i] = new KeenModel.Zone(KeenModel.Zone.Type.ADD,val,i);
                    break;
                case 'm':
                    zones[i] = new KeenModel.Zone(KeenModel.Zone.Type.TIMES,val,i);
                    break;
                case 's':
                    zones[i] = new KeenModel.Zone(KeenModel.Zone.Type.MINUS,val,i);
                    break;
                case 'd':
                    zones[i] = new KeenModel.Zone(KeenModel.Zone.Type.DIVIDE,val,i);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported zone op: " + sym);
            }
            zoneClues[i] = clueOpForSymbol(sym) | (long) val;
        }

        levelAsString = levelAsString.substring(zoneCount*7);
        if (levelAsString.startsWith(";")) {
            levelAsString = levelAsString.substring(1);
        }
        if (levelAsString.startsWith("S")) {
            levelAsString = levelAsString.substring(1);
        }

        class zonePairing
        {
            private int raw;
            private int real;

            private zonePairing(int a, int b)
            {
                raw = a; real = b;
            }
        }

        int[] dsf = new int[size * size];
        long[] clues = new long[size * size];
        int[] zoneRoots = new int[zoneCount];
        Arrays.fill(zoneRoots, -1);

        ArrayList<zonePairing> zonePairingList = new ArrayList<>();
        zonePairingList.add(new zonePairing(0,0));

        int nextZoneIndex = 1;


        for(int i = 0; i<size*size; ++i)
        {

            int val = Integer.parseInt(levelAsString.substring(i,i+1));
            int zone = Integer.parseInt(ZoneData.substring(0,2));
            ZoneData = ZoneData.substring(3);

            boolean exists = false;
            int zoneIndex = 0;

            for(int j = 0; j<zonePairingList.size(); ++j)
            {

                if(zone == zonePairingList.get(j).raw)
                {

                    exists = true;
                    zoneIndex = zonePairingList.get(j).real;
                    break;
                }

            }

            if(!exists)
            {
                zonePairingList.add(new zonePairing(zone,nextZoneIndex));
                zoneIndex = nextZoneIndex;
                nextZoneIndex++;
            }

            int x = i/size;
            int y = i%size;

            cells[x][y] = new KeenModel.GridCell(val,zones[zoneIndex]);

            int cellIndex = i;
            if (zoneIndex >= 0 && zoneIndex < zoneRoots.length) {
                if (zoneRoots[zoneIndex] == -1) {
                    zoneRoots[zoneIndex] = cellIndex;
                }
                dsf[cellIndex] = zoneRoots[zoneIndex];
            }

        }

        for (int i = 0; i < zoneCount; i++) {
            int root = zoneRoots[i];
            if (root >= 0 && root < clues.length) {
                clues[root] = zoneClues[i];
            }
        }

        return new KeenModel(size, zones, cells, dsf, clues);

    }

    private static long clueOpForSymbol(char sym) {
        switch (sym) {
            case 'a':
                return 0x00000000L; // ADD
            case 'm':
                return 0x20000000L; // MUL
            case 's':
                return 0x40000000L; // SUB
            case 'd':
                return 0x60000000L; // DIV
            default:
                throw new IllegalArgumentException("Unsupported zone op: " + sym);
        }
    }

    static {
        System.loadLibrary("keen-android-jni");
    }

    @SuppressWarnings("JniMissingFunction") //it exists, studio just does not recognize it...
    public native String getLevelFromC(
        int size,
        int diff,
        int multOnly,
        long seed,
        int modeFlags,
        int profileId
    );
}
