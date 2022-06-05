package nextstep.subway.dto;

import nextstep.subway.domain.Line;
import nextstep.subway.domain.LineStation;

import java.util.List;
import java.util.stream.Collectors;

public class LineResponse {
    private Long id;
    private String name;
    private String color;
    private List<StationResponse> stations;

    private LineResponse(Long id, String name, String color, List<LineStation> stations) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.stations = stations.stream().map(StationResponse::of).collect(Collectors.toList());
    }

    public static LineResponse of(Line line) {
        return new LineResponse(line.getId(), line.getName(), line.getColor(), line.getLineStations());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public List<StationResponse> getStations() {
        return stations;
    }
}
