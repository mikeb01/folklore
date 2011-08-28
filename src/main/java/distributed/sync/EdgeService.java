package distributed.sync;

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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class EdgeService
{
    private static final Logger LOG = Logger.getLogger(EdgeService.class.getName());
    
    private final int port;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<Long, Session> outgoingChannels = new HashMap<Long, Session>();
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
                    Session session = new Session(sequence++);
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
        private final ByteBuffer request = ByteBuffer.allocate(136);
        
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
                
                while (!currentThread.isInterrupted())
                {
                    request.clear();
                    
                    request.putLong(session.id);
                    if (-1 != channel.read(request))
                    {
                        if (0 == request.remaining())
                        {
                            request.flip();
                            datagramChannel.write(request);
                            
                            session.await();
                            while (0 != session.response.remaining())
                            {
                                if (-1 == channel.write(session.response))
                                {
                                    channel.close();
                                }
                            }
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
                        outgoingChannels.put(session.id, session);
                    }
                    
                    long id = buffer.getLong();
                    Session session = outgoingChannels.get(id);
                    
                    session.response.clear();
                    session.response.put(buffer);
                    session.response.flip();
                    session.signal();
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
        private final Lock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();
        public final long id;
        public ByteBuffer response = ByteBuffer.allocate(128);

        public Session(long id)
        {
            this.id = id;
        }

        public void signal()
        {
            lock.lock();
            try
            {
                condition.signal();
            }
            finally
            {
                lock.unlock();
            }
        }

        public void await() throws InterruptedException
        {
            lock.lock();
            try
            {
                condition.await();
            }
            finally
            {
                lock.unlock();
            }
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
