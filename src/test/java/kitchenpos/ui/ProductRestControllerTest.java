package kitchenpos.ui;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuGroup;
import kitchenpos.domain.MenuProduct;
import kitchenpos.domain.Product;
import kitchenpos.objectmother.MenuGroupMaker;
import kitchenpos.objectmother.MenuMaker;
import kitchenpos.objectmother.MenuProductMaker;
import kitchenpos.objectmother.ProductMaker;
import kitchenpos.ui.utils.ControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.UUID;

import static kitchenpos.objectmother.ProductMaker.*;
import static kitchenpos.ui.requestor.MenuGroupRequestor.메뉴그룹생성요청_메뉴그룹반환;
import static kitchenpos.ui.requestor.MenuRequestor.메뉴생성요청_메뉴반환;
import static kitchenpos.ui.requestor.MenuRequestor.메뉴전체조회요청;
import static kitchenpos.ui.requestor.ProductRequestor.*;
import static org.assertj.core.api.Assertions.assertThat;

class ProductRestControllerTest extends ControllerTest {

    @DisplayName("상품생성 후 상품조회시 추가한 상품이 조회되야 한다.")
    @Test
    void 상품생성() {
        // when
        ExtractableResponse<Response> response = 상품생성요청(상품_1);

        // then
        상품생성됨(response);
    }

    @DisplayName("상품생성 시 가격이 0원보다 작을경우 에러를 던진다.")
    @Test
    void 상품생성실패_가격음수() {
        // when
        ExtractableResponse<Response> response = 상품생성요청(음수가격상품);

        // then
        상품생성실패됨(response);
    }

    @DisplayName("상품생성 시 가격이름에 욕설이 포함되있을경우 에러를 던진다.")
    @Test
    void 상품생성실패_가격이름_욕설포함() {
        // when
        ExtractableResponse<Response> response = 상품생성요청(욕설상품);

        // then
        상품생성실패됨(response);
    }

    @DisplayName("상품가격변경시 해당 상품조회시 변경된 가격이 조회되야 한다.")
    @Test
    void 상품가격변경() {
        // given
        UUID 상품식별번호 = 상품생성요청_상품식별번호반환(상품_1);

        // when
        상품가격변경요청(상품식별번호, 상품_2);

        // then
        ExtractableResponse<Response> response = 상품전체조회요청();
        상품가격변경됨(response);
    }

    @DisplayName("상품가격변경시 해당 상품으로 구성된 메뉴가격이 메뉴상품 총 가격을 " +
            "초과할 경우 해당 메뉴를 비활성화 해야 한다.")
    @Test
    void 상품가격변경_메뉴가격_메뉴상품총가격_초과() {
        // given
        MenuGroup 메뉴그룹 = 메뉴그룹생성요청_메뉴그룹반환(MenuGroupMaker.make("메뉴그룹"));
        Product 상품_1 = 상품생성요청_상품반환(ProductMaker.make("상품1", 3000L));
        MenuProduct 메뉴상품_1 = MenuProductMaker.make(상품_1, 2);
        메뉴생성요청_메뉴반환(MenuMaker.make("메뉴1", 6000L, 메뉴그룹, 메뉴상품_1));

        // when
        상품가격변경요청(상품_1.getId(), make("상품1", 1000L));

        // then
        메뉴가격_메뉴상품총가격_초과메뉴_비활성화됨();
    }

    private void 상품생성됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    private void 상품생성실패됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private void 상품가격변경됨(ExtractableResponse<Response> response) {
        assertThat(response.jsonPath().getList("price", BigDecimal.class)
                .get(0)
                .compareTo(상품_2.getPrice())).isEqualTo(0);
    }

    private void 메뉴가격_메뉴상품총가격_초과메뉴_비활성화됨() {
        ExtractableResponse<Response> response = 메뉴전체조회요청();
        assertThat(response.jsonPath().getList("$", Menu.class))
                .hasSize(1)
                .first()
                .extracting(Menu::isDisplayed)
                .isEqualTo(false);
    }

}