package ch.sebpiller.babyphone.detection;


public record Detected(String type, float score, int x, int y, int width, int height) implements Comparable<Detected> {

    @Override
    public int compareTo(Detected o) {
        return Float.compare(this.score, o.score);
    }

    @Override
    public String toString() {
        return "Detected{" +
                "type='" + type + '\'' +
                ", score=" + score +
                '}';
    }
}