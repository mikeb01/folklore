package distributed.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class EdgeService
{
    private static final Logger LOG = Logger.getLogger(EdgeService.class.getName());
    
    private final int port;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<Long, SocketChannel> outgoingChannels = new HashMap<Long, SocketChannel>();
    private final Queue<Session> newConnections = new ConcurrentLinkedQueue<Session>();
    private final InetSocketAddress serviceAddress;
    private long sequence = 0;

    private DatagramChannel datagramChannel;


    public EdgeService(int port, String serviceHost, int servicePort) throws IOException
    {
        this.port = port;
        
        serviceAddress = new InetSocketAddress(serviceHost, servicePort);
        datagramChannel = DatagramChannel.open();
        datagramChannel.connect(serviceAddress);
    }
    
    public void run()
    {
        try
        {
            executor.execute(new ResponseHandler());
            Acceptor acceptor = new Acceptor(port);
            LOG.info("Starting acceptor");
            acceptor.run();
            
            executor.shutdown();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private class Acceptor implements Runnable
    {
        private final ServerSocketChannel serverSocket;
        private final int port;
        
        public Acceptor(int port) throws IOException
        {
            this.port = port;
            serverSocket = ServerSocketChannel.open();
        }
        
        @Override
        public void run()
        {            
            Thread currentThread = Thread.currentThread();
            try
            {
                serverSocket.socket().bind(new InetSocketAddress(port));
                
                while (!currentThread.isInterrupted())
                {
                    SocketChannel channel = serverSocket.accept();
                    LOG.info("Connection from: " + channel.socket().getRemoteSocketAddress());
                    Session session = new Session(sequence++, channel);
                    newConnections.add(session);
                    
                    executor.execute(new RequestHandler(session, channel));
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    private class RequestHandler implements Runnable
    {
        private final Session session;
        private final SocketChannel channel;
        
        public RequestHandler(Session session, SocketChannel channel) throws IOException
        {
            this.session = session;
            this.channel = channel;
        }
        
        @Override
        public void run()
        {
            try
            {
                Thread currentThread = Thread.currentThread();
                ByteBuffer buffer = session.readBuffer;
                
                while (!currentThread.isInterrupted())
                {
                    if (-1 != channel.read(buffer))
                    {
                        LOG.fine("Transferring Request");
                        if (0 == buffer.remaining())
                        {
                            buffer.flip();
                            datagramChannel.write(buffer);
                            session.reset();
                        }
                    }
                    else
                    {
                        channel.close();
                        break;
                    }                    
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
    private class ResponseHandler implements Runnable
    {
        private final ByteBuffer buffer = ByteBuffer.allocate(136);
        
        public ResponseHandler() throws IOException
        {
        }
        
        @Override
        public void run()
        {
            try
            {
                Thread currentThread = Thread.currentThread();
                
                while (!currentThread.isInterrupted())
                {
                    buffer.clear();
                    
                    while (0 != buffer.remaining())
                    {                        
                        datagramChannel.read(buffer);
                    }
                    buffer.flip();
                    
                    for (Session session : newConnections)
                    {
                        outgoingChannels.put(session.id, session.channel);
                    }
                    
                    long id = buffer.getLong();
                    SocketChannel channel = outgoingChannels.get(id);
                    LOG.fine("Transferring Response");
                    while (0 != buffer.remaining())
                    {
                        if (-1 == channel.write(buffer))
                        {
                            channel.close();
                        }
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    private static class Session
    {
        public final long id;
        public final SocketChannel channel;
        public ByteBuffer readBuffer = ByteBuffer.allocate(136);

        public Session(long id, SocketChannel channel)
        {
            this.id = id;
            this.channel = channel;
            reset();
        }
        
        public void reset()
        {
            readBuffer.clear();
            readBuffer.putLong(id);
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        if (args.length != 3)
        {
            throw new RuntimeException("Usage async.server.edge <port> <service host> <service port>");
        }
        
        int port = Integer.parseInt(args[0]);
        String serviceHost = args[1];
        int servicePort = Integer.parseInt(args[2]);
        
        EdgeService edgeService = new EdgeService(port, serviceHost, servicePort);
        edgeService.run();
    }
}
