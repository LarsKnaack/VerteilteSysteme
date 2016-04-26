package aqua.broker;

import aqua.common.Direction;
import aqua.common.FishModel;
import aqua.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {
    private static int counter = 0;
    private static boolean done = false;
    ExecutorService executor = Executors.newFixedThreadPool(6);
    ReadWriteLock lock = new ReentrantReadWriteLock();
    private Endpoint endpoint = new Endpoint(4711);
    private ClientCollection<InetSocketAddress> collection = new ClientCollection<InetSocketAddress>();

    public static void main(final String[] args) {
        Broker broker = new Broker();
        System.out.println("Broker is running...");

        Thread stopThread = new Thread(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, "Press OK to stop server");
                Broker.done = true;
            }
        });

        //stopThread.start();
        broker.broker();
    }

    private void broker() {
        while (!done) {
            Message msg = endpoint.blockingReceive();
            if (msg.getPayload() instanceof PoisonPill) {
                break;
            }
            executor.execute(new BrokerTask(msg));
        }
        executor.shutdown();
        System.exit(0);
    }

    private class BrokerTask implements Runnable {
        private Message message;

        BrokerTask(Message msg) {
            this.message = msg;
        }

        @Override
        public void run() {
            if (message.getPayload() instanceof RegisterRequest) {
                register();
            } else if (message.getPayload() instanceof DeregisterRequest) {
                deregister();
            } else if (message.getPayload() instanceof HandoffRequest) {
                handofffish();
            }
        }

        private void handofffish() {
            HandoffRequest request = (HandoffRequest) message.getPayload();
            FishModel fish = request.getFish();
            Direction direction = fish.getDirection();
            lock.readLock().lock();
            int index = collection.indexOf(message.getSender());
            lock.readLock().unlock();
            InetSocketAddress receiver = null;
            lock.readLock().lock();
            if (direction == Direction.LEFT) {
                receiver = collection.getLeftNeighorOf(index);
            } else {
                receiver = collection.getRightNeighorOf(index);
            }
            lock.readLock().unlock();
            endpoint.send(receiver, request);
        }

        private void deregister() {
            DeregisterRequest request = (DeregisterRequest) message.getPayload();
            String senderId = request.getId();
            lock.writeLock().lock();
            int index = collection.indexOf(senderId);
            collection.remove(index);
            lock.writeLock().unlock();
            lock.readLock().lock();
            index = collection.indexOf(senderId);
            InetSocketAddress left = collection.getLeftNeighorOf(index);
            InetSocketAddress right = collection.getRightNeighorOf(index);
            lock.readLock().unlock();
            endpoint.send(left, new NeighbourUpdate(null, right));
            endpoint.send(right, new NeighbourUpdate(left, null));

        }

        private void register() {
            InetSocketAddress sender = message.getSender();
            String clientId = "client" + counter;
            lock.writeLock().lock();
            collection.add(clientId, sender);
            lock.writeLock().unlock();
            lock.readLock().lock();
            int index = collection.indexOf(sender);
            InetSocketAddress left = collection.getLeftNeighorOf(index);
            InetSocketAddress right = collection.getRightNeighorOf(index);
            lock.readLock().unlock();
            RegisterResponse response = new RegisterResponse(clientId);
            endpoint.send(sender, response);
            endpoint.send(sender, new NeighbourUpdate(left, right));
            endpoint.send(left, new NeighbourUpdate(null, sender));
            endpoint.send(right, new NeighbourUpdate(sender, null));

            if (counter == 0) {
                endpoint.send(sender, new Token());
            }
            counter++;
        }
    }


}
