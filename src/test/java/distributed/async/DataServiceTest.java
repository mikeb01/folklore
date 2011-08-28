package distributed.async;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

import distributed.DataService;

public class DataServiceTest
{

    @Test
    public void shouldReverseByteBuffer()
    {
        ByteBuffer toReverse = ByteBuffer.allocate(1024);
        toReverse.putInt(16).putLong(1234L);
        putString(toReverse, "12345678");
        
        toReverse.putInt(18).putLong(1234L);
        putString(toReverse, "1234567890");

        toReverse.putInt(14).putLong(1234L);
        putString(toReverse, "123456");
        toReverse.flip();
        
        ByteBuffer expected = ByteBuffer.allocate(1024);
        expected.putInt(16).putLong(1234L);
        putString(expected, "87654321");
        
        expected.putInt(18).putLong(1234L);
        putString(expected, "0987654321");

        expected.putInt(14).putLong(1234L);
        putString(expected, "654321");
        expected.flip();
        
        ByteBuffer output = ByteBuffer.allocate(1024);
        
        DataService ds = new DataService(10000);
        ds.reverse(toReverse, output);
        
        output.flip();
        
        assertThat(true, is(Arrays.equals(expected.array(), output.array())));
    }

    private void putString(ByteBuffer toReverse, String string)
    {
        for (int i = 0; i < string.length(); i++)
        {
            toReverse.put((byte) string.charAt(i));
        }
    }

}
