package com.dishanhai.ftb_quest_nexus.compat.create;

/** A single live Create network measurement (SU, not energy or a transferable resource). */
record StressReading(double capacity, double load) {
    enum Metric {
        CAPACITY, LOAD, HEADROOM;
        double value(StressReading reading) {
            return switch (this) {
                case CAPACITY -> reading.capacity;
                case LOAD -> reading.load;
                case HEADROOM -> Math.max(0, reading.capacity - reading.load);
            };
        }
    }

    static StressReading active(double speed, double capacity, double load, boolean connected,
                                boolean dirty, boolean overloaded) {
        if (!connected || dirty || overloaded || !Double.isFinite(speed) || speed == 0
                || !Double.isFinite(capacity) || !Double.isFinite(load) || capacity <= 0 || load < 0
                || load > capacity) return null;
        return new StressReading(capacity, load);
    }

    boolean reaches(Metric metric, long amount) { return metric.value(this) >= amount; }
}
