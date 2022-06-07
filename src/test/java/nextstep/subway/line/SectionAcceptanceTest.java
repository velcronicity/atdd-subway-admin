package nextstep.subway.line;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.domain.Line;
import nextstep.subway.domain.LineRepository;
import nextstep.subway.domain.Station;
import nextstep.subway.domain.StationRepository;
import nextstep.subway.dto.SectionRequest;
import nextstep.subway.utils.AcceptanceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("구간 관련 기능")
public class SectionAcceptanceTest extends AcceptanceTest {
    @Autowired
    private StationRepository stationRepository;
    @Autowired
    private LineRepository lineRepository;

    private Station upStation;
    private Station downStation;
    private Station newUpStation;
    private Station newDownStation;
    private Station newStation;
    private Line line;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        newUpStation = stationRepository.save(new Station("신사"));
        newDownStation = stationRepository.save(new Station("광교"));
        upStation = stationRepository.save(new Station("강남"));
        downStation = stationRepository.save(new Station("광교중앙"));
        newStation = stationRepository.save(new Station("양재"));
        line = lineRepository.save(new Line("신분당선", "bg-red-600", 2, upStation, downStation));
    }

    /**
     * When 지하철_노선에_지하철역_등록_요청
     * Then  지하철_노선에_지하철역_등록됨
     */
    @DisplayName("노선에 구간을 등록한다.")
    @Test
    void addSection() {
        // when
        ExtractableResponse<Response> response = addSection(line.getId(),
                new SectionRequest(upStation.getId(), downStation.getId(), newStation.getId(), 1));

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // then
        List<String> stationNames = getOne(line.getId()).jsonPath().getList("stations.name");
        assertThat(stationNames).containsExactlyInAnyOrder("강남", "양재", "광교중앙");
    }

    /**
     * When 지하철_노선에_지하철역_등록_요청
     * Then  지하철_노선에_지하철역_실패함
     */
    @DisplayName("노선에 기존 역 사이 길이보다 크거나 같은 구간을 등록하면 실패한다.")
    @Test
    void addSection_exceed() {
        // when
        ExtractableResponse<Response> response = addSection(line.getId(),
                new SectionRequest(upStation.getId(), downStation.getId(), newStation.getId(), 2));

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * When 지하철_노선에_지하철역_등록_요청
     * Then  지하철_노선에_지하철역_실패함
     */
    @DisplayName("노선에 이미 존재하는 구간을 등록하면 실패한다.")
    @Test
    void addSection_already_added() {
        // when
        ExtractableResponse<Response> response = addSection(line.getId(),
                new SectionRequest(null, downStation.getId(), upStation.getId(), 1));

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * When 지하철_노선에_지하철역_등록_요청
     * Then  지하철_노선에_지하철역_실패함
     */
    @DisplayName("노선에 상행역과 하행역 둘 중 하나도 포함되어있지 않은 구간을 등록하면 실패한다.")
    @Test
    void addSection_empty() {
        // when
        ExtractableResponse<Response> response = addSection(line.getId(),
                new SectionRequest(null, null, newStation.getId(), 1));

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * When 지하철_노선에_지하철역_등록_요청
     * Then  지하철_노선에_지하철역_실패함
     */
    @DisplayName("노선에 상행역과 하행역이 같은 구간을 등록하면 실패한다.")
    @Test
    void addSection_same_station() {
        // when
        ExtractableResponse<Response> response = addSection(line.getId(),
                new SectionRequest(downStation.getId(), downStation.getId(), newStation.getId(), 1));

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * When 지하철_노선에_지하철역_등록_요청
     * Then  지하철_노선에_지하철역_등록됨
     */
    @DisplayName("노선에 상행 종점 구간을 등록한다.")
    @Test
    void addAscendEndpointSection() {
        // when
        ExtractableResponse<Response> response =
                addSection(line.getId(), new SectionRequest(null, upStation.getId(), newUpStation.getId(), 5));

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // then
        List<String> stationNames = getOne(line.getId()).jsonPath().getList("stations.name");
        assertThat(stationNames).containsExactlyInAnyOrder("신사", "강남", "광교중앙");
    }

    /**
     * When 지하철_노선에_지하철역_등록_요청
     * Then  지하철_노선에_지하철역_등록됨
     */
    @DisplayName("노선에 하행 종점 구간을 등록한다.")
    @Test
    void addDescendEndpointSection() {
        // when
        ExtractableResponse<Response> response =
                addSection(line.getId(), new SectionRequest(downStation.getId(), null, newDownStation.getId(), 5));

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // then
        List<String> stationNames = getOne(line.getId()).jsonPath().getList("stations.name");
        assertThat(stationNames).containsExactlyInAnyOrder("강남", "광교중앙", "광교");
    }

    private ExtractableResponse<Response> addSection(long lineId, SectionRequest sectionRequest) {
        return RestAssured.given().log().all().body(sectionRequest).contentType(MediaType.APPLICATION_JSON_VALUE).when()
                .post("/lines/{lineId}/sections", lineId).then().log().all().extract();
    }

    private ExtractableResponse<Response> getOne(Long lineId) {
        return RestAssured.given().log().all().when().get("/lines/{lineId}", lineId).then().log().all().extract();
    }

}