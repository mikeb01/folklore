package distributed.async;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.logging.Logger;

public class DataService implements Runnable
{
    private static final Logger LOG = Logger.getLogger(DataService.class.getName());
    private final int port;
    private volatile Thread thread = null;

    public DataService(int port)
    {
        this.port = port;
    }

    @Override
    public void run()
    {
        try
        {
            ByteBuffer input = ByteBuffer.allocate(1400);
            ByteBuffer output = ByteBuffer.allocate(1400);
            SocketAddress address = new InetSocketAddress(port);
            DatagramChannel channel = DatagramChannel.open();
            DatagramSocket socket = channel.socket();
            socket.bind(address);
            LOG.info("Bound on port: " + address);
            
            thread = Thread.currentThread();
            while (!thread.isInterrupted())
            {
                input.clear();
                output.clear();
                
                SocketAddress clientAddress = channel.receive(input);
                input.flip();
                reverse(input, output);
                channel.send(output, clientAddress);                
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void reverse(ByteBuffer input, ByteBuffer output)
    {
        output.putLong(input.getLong());
        
        int i = 1;
        while (0 != input.remaining())
        {
            output.put(input.limit() - i, input.get());
            i++;
        }
        
        output.position(0).limit(input.position());
    }

    public static void main(String[] args)
    {
        DataService dataService = new DataService(10001);
        dataService.run();
    }
}
