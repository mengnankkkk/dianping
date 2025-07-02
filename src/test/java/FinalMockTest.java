import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;

final class FinalClass {
    String hello() { return "hello"; }
}

@ExtendWith(MockitoExtension.class)
public class FinalMockTest {
    @Mock
    FinalClass finalClass;

    @Test
    public void testFinalMock() {
        Mockito.when(finalClass.hello()).thenReturn("mocked");
        assertEquals("mocked", finalClass.hello());
    }
}