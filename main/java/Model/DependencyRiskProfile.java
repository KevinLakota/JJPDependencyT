package Model;

public class DependencyRiskProfile implements Comparable<DependencyRiskProfile> {
    private final String name;
    private final String version;
    private final int criticalCount;
    private final int highCount;
    private final int mediumCount;
    private final double maxCriticalScore;
    private final double maxHighScore;
    private final double maxMediumScore;
    private final long frequency;

    public DependencyRiskProfile(String name, String version,
                                 int criticalCount, int highCount, int mediumCount,
                                 double maxCriticalScore, double maxHighScore, double maxMediumScore,
                                 long frequency) {
        this.name = name;
        this.version = version;
        this.criticalCount = criticalCount;
        this.highCount = highCount;
        this.mediumCount = mediumCount;
        this.maxCriticalScore = maxCriticalScore;
        this.maxHighScore = maxHighScore;
        this.maxMediumScore = maxMediumScore;
        this.frequency = frequency;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public int getCriticalCount() {
        return criticalCount;
    }

    public int getHighCount() {
        return highCount;
    }

    public int getMediumCount() {
        return mediumCount;
    }

    public double getMaxCriticalScore() {
        return maxCriticalScore;
    }

    public double getMaxHighScore() {
        return maxHighScore;
    }

    public double getMaxMediumScore() {
        return maxMediumScore;
    }

    public long getFrequency() {
        return frequency;
    }

    @Override
    public int compareTo(DependencyRiskProfile other) {
        // 1. Porovnanie počtu Critical
        if (this.criticalCount != other.criticalCount) {
            return Integer.compare(other.criticalCount, this.criticalCount);
        }
        // 2. Porovnanie najvyššieho Critical skóre
        if (Double.compare(this.maxCriticalScore, other.maxCriticalScore) != 0) {
            return Double.compare(other.maxCriticalScore, this.maxCriticalScore);
        }
        // 3. Porovnanie počtu High
        if (this.highCount != other.highCount) {
            return Integer.compare(other.highCount, this.highCount);
        }
        // 4. Porovnanie najvyššieho High skóre
        if (Double.compare(this.maxHighScore, other.maxHighScore) != 0) {
            return Double.compare(other.maxHighScore, this.maxHighScore);
        }
        // 5. Porovnanie počtu Medium
        if (this.mediumCount != other.mediumCount) {
            return Integer.compare(other.mediumCount, this.mediumCount);
        }
        // 6. Porovnanie najvyššieho Medium skóre
        if (Double.compare(this.maxMediumScore, other.maxMediumScore) != 0) {
            return Double.compare(other.maxMediumScore, this.maxMediumScore);
        }
        // 7. Porovnanie frekvencie výskytu
        return Long.compare(other.frequency, this.frequency);
    }

    @Override
    public String toString() {
        return "DependencyRiskProfile{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", criticalCount=" + criticalCount +
                ", highCount=" + highCount +
                ", mediumCount=" + mediumCount +
                ", maxCriticalScore=" + maxCriticalScore +
                ", maxHighScore=" + maxHighScore +
                ", maxMediumScore=" + maxMediumScore +
                ", frequency=" + frequency +
                '}';
    }
}
