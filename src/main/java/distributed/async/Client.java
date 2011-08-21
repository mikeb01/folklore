package distributed.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import distributed.async.TimedRunner.Iteration;

public class Client
{
    private static final Logger LOG = Logger.getLogger(Client.class.getName());
    private static final byte[] body = 
            ("this is a long string that will be used as the data package for this test." +
            " xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx").getBytes();
    private final String host;
    private final int port;
    private final int throughput;
    private final int iterations;
    private final long[] startTimestamps;
    private final long[] endTimestamps;
    private SocketChannel channel;

    public Client(String host, int port, int throughput)
    {
        this.host = host;
        this.port = port;
        this.throughput = throughput;
        this.iterations = throughput * 60;
        startTimestamps = new long[(int) iterations];
        endTimestamps = new long[(int) iterations];
    }

    private void run()
    {
        try
        {
            channel = SocketChannel.open();
            channel.connect(new InetSocketAddress(host, port));
            
            Thread senderThread = new Thread(new Sender());
            Thread receiverThread = new Thread(new Receiver());
            
            senderThread.start();
            receiverThread.start();
            
            senderThread.join();
            receiverThread.join();
            
            DescriptiveStatistics ds = new DescriptiveStatistics();
            for (int i = 0, n = startTimestamps.length; i < n; i++)
            {
                ds.addValue(endTimestamps[i] - startTimestamps[i]);
            }
            
            LOG.info("Mean: " + ds.getMean());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    private class Sender implements Runnable
    {
        @Override
        public void run()
        {
            final ByteBuffer output = ByteBuffer.allocate(128);
            output.putInt(124);
            output.putLong(12345);
            output.put(body);
            LOG.info("Started Sender");
            
            TimedRunner.run(iterations, throughput, new Iteration()
            {
                @Override
                public boolean run(int i)
                {
                    output.clear();
                    startTimestamps[i] = System.nanoTime();
                    
                    while (0 != output.remaining())
                    {
                        try
                        {
                            LOG.fine("Sending message");
                            channel.write(output);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            return false;
                        }
                    }
                    
                    return true;
                }
            });
        }
    }
    
    private class Receiver implements Runnable
    {
        @Override
        public void run()
        {
            LOG.info("Started Receiver");
            final ByteBuffer input = ByteBuffer.allocate(128);
            int i = iterations;
            
            try
            {
                while (-1 != --i)
                {
                    input.clear();
                    
                    while (input.remaining() != 0)
                    {
                        LOG.fine("Receiving message");
                        channel.read(input);
                    }
                    
                    endTimestamps[i] = System.nanoTime();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args)
    {
        System.out.println(body.length);
        
        if (args.length != 3)
        {
            throw new RuntimeException("Usage <host> <port> <throughput ops/sec>");
        }
        
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int throughput = Integer.parseInt(args[2]);
        
        Client client = new Client(host, port, throughput);
        
        client.run();
        
    }
}
