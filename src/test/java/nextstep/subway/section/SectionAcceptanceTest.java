package nextstep.subway.section;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import nextstep.subway.line.LineAcceptanceTest;
import nextstep.subway.station.StationAcceptanceTest;
import nextstep.subway.station.dto.StationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철 구간 관련 기능")
public class SectionAcceptanceTest extends AcceptanceTest {
	private Map<String, String> createParams;

	private StationResponse 강남역;
	private StationResponse 광교역;

	private ExtractableResponse<Response> 신분당선;

	@BeforeEach
	void setBeforeEach() {
		super.setUp();

		// given
		강남역 = StationAcceptanceTest.지하철역을_생성한다("강남역").as(StationResponse.class);
		광교역 = StationAcceptanceTest.지하철역을_생성한다("광교역").as(StationResponse.class);

		createParams = new HashMap<>();
		createParams.put("name", "신분당선");
		createParams.put("color", "bg-red-600");
		createParams.put("upStationId", 강남역.getId().toString());
		createParams.put("downStationId", 광교역.getId().toString());
		createParams.put("distance", "10");
		신분당선 = LineAcceptanceTest.노선을_생성한다(createParams);
	}

	@Test
	@DisplayName("노선에 구간을 등록한다.")
	void addSection() {
		// given
		StationResponse 양재역 = StationAcceptanceTest.지하철역을_생성한다("양재역").as(StationResponse.class);

		Map<String, String> sectionParams = new HashMap<>();
		sectionParams.put("upStationId", 강남역.getId().toString());
		sectionParams.put("downStationId", 양재역.getId().toString());
		sectionParams.put("distance", "3");

		// when
		ExtractableResponse<Response> response = 지하철_노선에_새로운_구간_등록_요청(신분당선.header("Location"), sectionParams);

		// then
		노선에_새로_등록된_역이_순서대로_조회된다(response, 강남역.getName(), 양재역.getName(), 광교역.getName());
	}

	private void 노선에_새로_등록된_역이_순서대로_조회된다(ExtractableResponse<Response> addSectionResponse, String... stations) {
		ExtractableResponse<Response> 신분당선_조회_결과 = LineAcceptanceTest.특정_노선을_조회한다(신분당선.header("Location"));
		assertThat(addSectionResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(신분당선_조회_결과.jsonPath().getList("stations.name")).containsExactly(Arrays.stream(stations).toArray());
	}

	private ExtractableResponse<Response> 지하철_노선에_새로운_구간_등록_요청(String resourcePath, Map<String, String> sectionParams) {
		return RestAssured.given().log().all()
				.body(sectionParams)
				.contentType(MediaType.APPLICATION_JSON_VALUE)
				.when()
				.post(resourcePath + "/sections")
				.then().log().all()
				.extract();
	}

	@Test
	@DisplayName("새로운 역을 상행 종점으로 등록한다")
	void addSection2() {
		// given
		StationResponse 강남위역 = StationAcceptanceTest.지하철역을_생성한다("강남위역").as(StationResponse.class);

		Map<String, String> sectionParams = new HashMap<>();
		sectionParams.put("upStationId", 강남위역.getId().toString());
		sectionParams.put("downStationId", 강남역.getId().toString());
		sectionParams.put("distance", "3");

		// when
		ExtractableResponse<Response> response = 지하철_노선에_새로운_구간_등록_요청(신분당선.header("Location"), sectionParams);

		// then
		노선에_새로_등록된_역이_순서대로_조회된다(response, 강남위역.getName(), 강남역.getName(), 광교역.getName());
	}

	@Test
	@DisplayName("새로운 역을 하행 종점으로 등록한다")
	void addSection3() {
		// given
		StationResponse 광교아래역 = StationAcceptanceTest.지하철역을_생성한다("광교아래역").as(StationResponse.class);

		Map<String, String> sectionParams = new HashMap<>();
		sectionParams.put("upStationId", 광교역.getId().toString());
		sectionParams.put("downStationId", 광교아래역.getId().toString());
		sectionParams.put("distance", "3");

		// when
		ExtractableResponse<Response> response = 지하철_노선에_새로운_구간_등록_요청(신분당선.header("Location"), sectionParams);

		// then
		노선에_새로_등록된_역이_순서대로_조회된다(response, 강남역.getName(), 광교역.getName(), 광교아래역.getName());
	}

	@Test
	@DisplayName("역 사이에 새로운 역을 등록할 경우 기존 역 사이 길이보다 크거나 같으면 등록을 할 수 없음")
	void checkDistance() {
		// given
		StationResponse 양재역 = StationAcceptanceTest.지하철역을_생성한다("양재역").as(StationResponse.class);

		Map<String, String> sectionParams = new HashMap<>();
		sectionParams.put("upStationId", 강남역.getId().toString());
		sectionParams.put("downStationId", 양재역.getId().toString());
		sectionParams.put("distance", "10");

		// when
		ExtractableResponse<Response> response = 지하철_노선에_새로운_구간_등록_요청(신분당선.header("Location"), sectionParams);

		// then
		노선에_역이_등록되지_않는다(response);
	}

	private void 노선에_역이_등록되지_않는다(ExtractableResponse<Response> response) {
		assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	@DisplayName("상행역과 하행역이 이미 노선에 모두 등록되어 있다면 익셉션 발생")
	void checkDuplication() {
		Map<String, String> sectionParams = new HashMap<>();
		sectionParams.put("upStationId", 강남역.getId().toString());
		sectionParams.put("downStationId", 광교역.getId().toString());
		sectionParams.put("distance", "1");

		// when
		ExtractableResponse<Response> response = 지하철_노선에_새로운_구간_등록_요청(신분당선.header("Location"), sectionParams);

		// then
		노선에_역이_등록되지_않는다(response);
	}

	@Test
	@DisplayName("상행역과 하행역 둘 중 하나도 포함되어있지 않으면 추가할 수 없다")
	void checkIncluded() {
		// given
		StationResponse 뚝섬역 = StationAcceptanceTest.지하철역을_생성한다("뚝섬역").as(StationResponse.class);
		StationResponse 서울숲역 = StationAcceptanceTest.지하철역을_생성한다("서울숲역").as(StationResponse.class);

		Map<String, String> sectionParams = new HashMap<>();
		sectionParams.put("upStationId", 뚝섬역.getId().toString());
		sectionParams.put("downStationId", 서울숲역.getId().toString());
		sectionParams.put("distance", "3");

		// when
		ExtractableResponse<Response> response = 지하철_노선에_새로운_구간_등록_요청(신분당선.header("Location"), sectionParams);

		// then
		노선에_역이_등록되지_않는다(response);
	}

	@Test
	@DisplayName("노선의 중간역을 제거한다")
	void removeSectionTest() {
		// given
		StationResponse 양재역 = StationAcceptanceTest.지하철역을_생성한다("양재역").as(StationResponse.class);
		Map<String, String> sectionParams = new HashMap<>();
		sectionParams.put("upStationId", 강남역.getId().toString());
		sectionParams.put("downStationId", 양재역.getId().toString());
		sectionParams.put("distance", "3");
		지하철_노선에_새로운_구간_등록_요청(신분당선.header("Location"), sectionParams);

		// when
		ExtractableResponse<Response> response = 지하철_노선에_구간_제거_요청(신분당선.header("Location"), 양재역.getId());

		// then
		노선에_제거된_역이_조회되지않는다(response, 양재역.getName());
	}

	@Test
	@DisplayName("노선의 마지막 역을 제거한다")
	void removeSectionTest2() {
		// given
		StationResponse 양재역 = StationAcceptanceTest.지하철역을_생성한다("양재역").as(StationResponse.class);
		Map<String, String> sectionParams = new HashMap<>();
		sectionParams.put("upStationId", 강남역.getId().toString());
		sectionParams.put("downStationId", 양재역.getId().toString());
		sectionParams.put("distance", "3");
		지하철_노선에_새로운_구간_등록_요청(신분당선.header("Location"), sectionParams);

		// when
		ExtractableResponse<Response> response = 지하철_노선에_구간_제거_요청(신분당선.header("Location"), 강남역.getId());

		// then
		노선에_제거된_역이_조회되지않는다(response, 강남역.getName());
	}

	private ExtractableResponse<Response> 지하철_노선에_구간_제거_요청(String resourcePath, Long stationId) {
		return RestAssured.given().log().all()
				.when()
				.delete(resourcePath + "/sections?stationId=" + stationId)
				.then().log().all()
				.extract();
	}

	private void 노선에_제거된_역이_조회되지않는다(ExtractableResponse<Response> response, String station) {
		ExtractableResponse<Response> 신분당선_조회_결과 = LineAcceptanceTest.특정_노선을_조회한다(신분당선.header("Location"));
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(신분당선_조회_결과.jsonPath().getList("stations.name")).doesNotContain(station);
	}
}
