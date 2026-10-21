package software.santodan.extension.nuviointro;

import java.util.regex.Pattern;

/** Pure request validation, independent of Android and of provider-specific ID resolvers. */
public final class Submission {
    public final String imdb;
    public final int season, episode;
    public Submission(String video, Integer season, Integer episode) {
        if (video == null || season == null || episode == null || season < 0 || episode < 1)
            throw new IllegalArgumentException("An IMDb episode with a known season and episode is required.");
        String[] parts = video.split(":", -1);
        if (!Pattern.matches("tt[0-9]+", parts[0]) || (parts.length != 1 && parts.length != 3))
            throw new IllegalArgumentException("This episode does not have an IMDb ID. Submission is unavailable.");
        if (parts.length == 3 && (!parts[1].equals(season.toString()) || !parts[2].equals(episode.toString())))
            throw new IllegalArgumentException("Episode identifiers disagree. Submission is unavailable.");
        this.imdb = parts[0]; this.season = season; this.episode = episode;
    }
    public static long time(String text) {
        String[] parts = text.trim().split(":", -1);
        if (parts.length < 2 || parts.length > 3) throw new IllegalArgumentException("Use MM:SS or HH:MM:SS.");
        long seconds = 0;
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].matches("[0-9]{1,4}")) throw new IllegalArgumentException("Use MM:SS or HH:MM:SS.");
            int value = Integer.parseInt(parts[i]);
            if (i > 0 && value > 59) throw new IllegalArgumentException("Minutes and seconds must be below 60.");
            seconds = seconds * 60 + value;
        }
        if (seconds > 86400) throw new IllegalArgumentException("Timestamp exceeds 24 hours.");
        return seconds;
    }
    public String json(String start, String end, String type) {
        return json(time(start) * 1000, time(end) * 1000, type);
    }
    public String json(long a, long b, String type) {
        if (a < 0 || b > 86400000L || b <= a) throw new IllegalArgumentException("End must be after start, within the video.");
        if (!type.equals("intro") && !type.equals("recap") && !type.equals("outro")) throw new IllegalArgumentException("Invalid segment type.");
        return "{\"imdb_id\":\"" + imdb + "\",\"season\":" + season + ",\"episode\":" + episode +
            ",\"start_sec\":" + (a / 1000.0) + ",\"end_sec\":" + (b / 1000.0) + ",\"start_ms\":" + a +
            ",\"end_ms\":" + b + ",\"segment_type\":\"" + type + "\"}";
    }
}
