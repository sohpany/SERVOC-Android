package com.servoc.app.chemcore;

import com.servoc.app.hardware.HazRobHardwareProfile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Versioned logical placement of the eight installed MQ heads inside the standard chamber.
 *
 * The current release intentionally freezes row/column identity and wiring correspondence only.
 * It does not invent physical x/y/z coordinates that have not yet been measured on the as-built
 * chamber. A future metrology pass may add measured coordinates under a new layout version.
 */
public final class SamplingLayout {
    public enum PositionStatus {
        DESIGN_LOGICAL,
        DESIGN_NOMINAL,
        AS_BUILT_MEASURED
    }

    public static final String STANDARD_V2_LAYOUT_ID = "hazrob-chemcore-array-layout-v2";

    public static final class Slot {
        public final String sensorKey;
        public final int row;
        public final int column;
        public final int adsAddress;
        public final int adcChannel;

        Slot(String sensorKey, int row, int column, int adsAddress, int adcChannel) {
            this.sensorKey = sensorKey;
            this.row = row;
            this.column = column;
            this.adsAddress = adsAddress;
            this.adcChannel = adcChannel;
        }
    }

    public final String layoutId;
    public final String samplingProfileId;
    public final PositionStatus positionStatus;
    public final int rows;
    public final int columns;
    public final List<Slot> slots;
    public final String note;

    private SamplingLayout(
            String layoutId,
            String samplingProfileId,
            PositionStatus positionStatus,
            int rows,
            int columns,
            List<Slot> slots,
            String note) {
        this.layoutId = layoutId;
        this.samplingProfileId = samplingProfileId;
        this.positionStatus = positionStatus;
        this.rows = rows;
        this.columns = columns;
        this.slots = Collections.unmodifiableList(slots);
        this.note = note == null ? "" : note;
        validate();
    }

    /**
     * Canonical 2 x 4 logical chamber layout. Row 0 follows ADS1/0x48 channel order and row 1
     * follows ADS2/0x49 channel order. This minimizes wiring ambiguity and makes every stored
     * fingerprint reproducible with respect to sensor identity and slot identity.
     */
    public static SamplingLayout hazRobStandardArrayV2() {
        return new SamplingLayout(
                STANDARD_V2_LAYOUT_ID,
                "hazrob-chemcore-chamber-v2",
                PositionStatus.DESIGN_LOGICAL,
                2,
                4,
                Arrays.asList(
                        slot("mq2", 0, 0),
                        slot("mq4", 0, 1),
                        slot("mq5", 0, 2),
                        slot("mq8", 0, 3),
                        slot("mq3", 1, 0),
                        slot("mq7", 1, 1),
                        slot("mq9", 1, 2),
                        slot("mq135", 1, 3)),
                "Canonical logical 2x4 arrangement tied to ADS channel order. Physical x/y/z " +
                        "coordinates are intentionally unspecified until measured on the as-built chamber."
        );
    }

    public Slot forSensor(String sensorKey) {
        if (sensorKey == null) return null;
        for (Slot slot : slots) {
            if (slot.sensorKey.equalsIgnoreCase(sensorKey)) return slot;
        }
        return null;
    }

    public Slot at(int row, int column) {
        for (Slot slot : slots) {
            if (slot.row == row && slot.column == column) return slot;
        }
        return null;
    }

    private static Slot slot(String sensorKey, int row, int column) {
        HazRobHardwareProfile.Channel hw = HazRobHardwareProfile.forKey(sensorKey);
        if (hw == null) throw new IllegalStateException("Unknown installed sensor: " + sensorKey);
        return new Slot(sensorKey, row, column, hw.adsAddress, hw.adcChannel);
    }

    private void validate() {
        if (layoutId == null || layoutId.trim().isEmpty()) throw new IllegalArgumentException("layoutId required");
        if (samplingProfileId == null || samplingProfileId.trim().isEmpty()) throw new IllegalArgumentException("samplingProfileId required");
        if (rows <= 0 || columns <= 0) throw new IllegalArgumentException("invalid layout dimensions");
        if (slots == null || slots.size() != HazRobHardwareProfile.MQ.length)
            throw new IllegalArgumentException("layout must contain every installed MQ sensor exactly once");

        boolean[][] occupied = new boolean[rows][columns];
        for (Slot slot : slots) {
            if (slot == null || slot.sensorKey == null) throw new IllegalArgumentException("invalid slot");
            if (slot.row < 0 || slot.row >= rows || slot.column < 0 || slot.column >= columns)
                throw new IllegalArgumentException("slot outside layout: " + slot.sensorKey);
            if (occupied[slot.row][slot.column])
                throw new IllegalArgumentException("duplicate chamber slot");
            occupied[slot.row][slot.column] = true;

            HazRobHardwareProfile.Channel hw = HazRobHardwareProfile.forKey(slot.sensorKey);
            if (hw == null || hw.adsAddress != slot.adsAddress || hw.adcChannel != slot.adcChannel)
                throw new IllegalArgumentException("slot/wiring mismatch: " + slot.sensorKey);
        }

        for (HazRobHardwareProfile.Channel hw : HazRobHardwareProfile.MQ) {
            int count = 0;
            for (Slot slot : slots) if (hw.key.equalsIgnoreCase(slot.sensorKey)) count++;
            if (count != 1) throw new IllegalArgumentException("sensor occurrence mismatch: " + hw.key);
        }
    }
}
