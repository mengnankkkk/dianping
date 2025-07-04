
import com.mengnankk.controller.ShopController;
import com.mengnankk.dto.Result;
import com.mengnankk.entity.Shop;
import com.mengnankk.service.ShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ShopControllerTest {

    @Mock
    private ShopService shopService;

    @InjectMocks
    private ShopController shopController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void queryShopById_ValidId_ReturnsResultFromService() {
        // Arrange
        Long shopId = 123L;
        Result expectedResult = Result.ok("Shop details");
        when(shopService.queryById(shopId)).thenReturn(expectedResult);

        // Act
        Result actualResult = shopController.queryShopById(shopId);

        // Assert
        assertEquals(expectedResult, actualResult);
        verify(shopService, times(1)).queryById(shopId);
    }

    @Test
    void queryShopById_ServiceThrowsException_ThrowsControllerException() {
        // Arrange
        Long shopId = 123L;

        // Mock the service to throw an exception with a specific message.
        when(shopService.queryById(shopId)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND , "店铺不存在"));

        // Act and Assert
        // Use assertThrows to verify that the controller method throws the expected exception.
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            shopController.queryShopById(shopId);
        });

        // Assert the exception message is correct
        assertEquals("404 NOT_FOUND \"店铺不存在\"", exception.getMessage());
    }

    @Test
    void queryShopList_ReturnsResultFromService() {
        // Arrange
        Result expectedResult = Result.ok("Shop list");
        when(shopService.queryShopList()).thenReturn(expectedResult);

        // Act
        Result actualResult = shopController.queryShopList();

        // Assert
        assertEquals(expectedResult, actualResult);
        verify(shopService, times(1)).queryShopList();
    }

    @Test
    void queryShopList_ServiceThrowsException_ThrowsControllerException() {
        // Arrange
        when(shopService.queryShopList()).thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR , "获取店铺列表失败"));

        // Act and Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            shopController.queryShopList();
        });

        // Assert the exception message is correct
        assertEquals("500 INTERNAL_SERVER_ERROR \"获取店铺列表失败\"", exception.getMessage());
    }

    @Test
    void updateShop_ValidShop_ReturnsResultFromService() {
        // Arrange
        Shop shop = new Shop();
        shop.setId(1L);
        Result expectedResult = Result.ok("Shop updated");
        when(shopService.updateShop(shop)).thenReturn(expectedResult);

        // Act
        Result actualResult = shopController.updateShop(shop);

        // Assert
        assertEquals(expectedResult, actualResult);
        verify(shopService, times(1)).updateShop(shop);
    }

    @Test
    void updateShop_ServiceThrowsException_ThrowsControllerException() {
        // Arrange
        Shop shop = new Shop();
        shop.setId(1L);
        when(shopService.updateShop(shop)).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST , "修改店铺信息失败"));

        // Act and Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            shopController.updateShop(shop);
        });

        // Assert the exception message is correct
        assertEquals("400 BAD_REQUEST \"修改店铺信息失败\"", exception.getMessage());
    }

}
