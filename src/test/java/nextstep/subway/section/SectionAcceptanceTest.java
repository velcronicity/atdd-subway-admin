package nextstep.subway.section;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.domain.Line;
import nextstep.subway.domain.Station;
import nextstep.subway.line.LineAcceptanceTest;
import nextstep.subway.station.StationAcceptanceTest;
import nextstep.subway.util.DatabaseCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철 구간 관련 기능")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SectionAcceptanceTest {
    private final LineAcceptanceTest lineAcceptanceTest = new LineAcceptanceTest();
    private final StationAcceptanceTest stationAcceptanceTest = new StationAcceptanceTest();

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @LocalServerPort
    int port;

    @BeforeEach
    public void setUp() {
        if (RestAssured.port == RestAssured.UNDEFINED_PORT) {
            RestAssured.port = port;
            databaseCleanup.afterPropertiesSet();
        }
        databaseCleanup.cleanUp();
    }

    /*
     * Given 지하철 노선을 생성하고
     * When 상행역이 기존 생성한 노선의 상행역과 동일하고 구간 길이가 더 짧은 새로운 지하철 구간을 생성하면
     * Then 새로운 길이를 뺀 나머지를 새롭게 추가된 역과의 길이로 설정된 지하철 구간이 생성된다.
     */
    @Test
    void 역_사이에_새로운_역으로_지하철구간_생성() {
        // given
        ExtractableResponse<Response> line = lineAcceptanceTest.createLine("신분당선", "bg-red-600", 10, "강남역", "판교역");

        // when
        ExtractableResponse<Response> response = createSection(line.jsonPath().getLong("id"), "강남역", "양재역", 7);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    /*
     * Given  지하철 노선을 생성하고
     * When 하행역이 기존 생성한 노선의 상행역과 동일한 새로운 지하철 구간을 생성하면
     * Then 지하철 구간이 생성된다.
     */
    @Test
    void 새로운_역을_상행_종점_지하철구간_생성() {
        // given
        ExtractableResponse<Response> line = lineAcceptanceTest.createLine("신분당선", "bg-red-600", 10, "강남역", "판교역");

        // when
        ExtractableResponse<Response> response = createSection(line.jsonPath().getLong("id"), "논현역", "강남역", 7);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    /*
     * Given 지하철 노선을 생성하고
     * When 상행역이 기존 생성한 노선의 하행역과 동일한 새로운 지하철 구간을 생성하면
     * Then 지하철 구간이 생성된다.
     */
    @Test
    void 새로운_역을_하행_종점으로_지하철구간_생성() {
        // given
        ExtractableResponse<Response> line = lineAcceptanceTest.createLine("신분당선", "bg-red-600", 10, "강남역", "판교역");

        // when
        ExtractableResponse<Response> response = createSection(line.jsonPath().getLong("id"), "판교역", "광교역", 7);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    /*
     * Given 지하철 노선을 생성하고
     * When 상행역이 기존 생성한 노선의 상행역과 동일하고 구간 길이가 같은 새로운 지하철 구간을 생성하면
     * Then 예외가 발생한다.
     */
    @Test
    @DisplayName("역 사이에 새로운 역을 등록할 경우 기존 역 사이 길이보다 크거나 같으면 등록을 할 수 없음")
    void 지하철구간_생성_예외_1() {

    }

    /*
     * Given 지하철 노선을 생성하고
     * When 기존 생성한 노선의 상행역과 하행역이 모두 동일한 새로운 지하철 구간을 생성하면
     * Then 예외가 발생한다.
     */
    @Test
    @DisplayName("상행역과 하행역이 이미 노선에 모두 등록되어 있다면 추가할 수 없음")
    void 지하철구간_생성_예외_2() {

    }

    /*
     * When 상행역 또는 하행역이 null인 지하철 구간을 생성하면
     * Then 예외가 발생한다.
     */
    @Test
    @DisplayName("상행역과 하행역 둘 중 하나도 포함되어있지 않으면 추가할 수 없음")
    void 지하철구간_생성_예외_3() {

    }

    private ExtractableResponse<Response> createSection(Long lineId, String upStationName, String downStationName, int distance) {
        ExtractableResponse<Response> upStation = stationAcceptanceTest.createStation(upStationName);
        ExtractableResponse<Response> downStation = stationAcceptanceTest.createStation(downStationName);

        Map<String, Object> param = new HashMap<>();
        param.put("upStationId", upStation.jsonPath().getLong("id"));
        param.put("downStationId", downStation.jsonPath().getLong("id"));
        param.put("distance", distance);

        return RestAssured.given().log().all()
                .body(param)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when().post("/lines/{id}/sections", lineId)
                .then().log().all()
                .extract();
    }
}
