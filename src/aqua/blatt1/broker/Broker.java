package aqua.blatt1.broker;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JOptionPane;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

public class Broker {
	private Endpoint endpoint = new Endpoint(4711);
	private ClientCollection<InetSocketAddress> collection = new ClientCollection<InetSocketAddress>();
	private static int counter = 0;
	ExecutorService executor = Executors.newFixedThreadPool(6);
	private static boolean done = false;
	ReadWriteLock  lock = new ReentrantReadWriteLock();
	
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
		
		stopThread.start();
		broker.broker();
	}

	private void broker() {
		while (!done) {
			Message msg = endpoint.blockingReceive();
			executor.execute(new BrokerTask(msg));
		}
		executor.shutdown();
	}

	private class BrokerTask implements Runnable {
		private Message message;
		BrokerTask (Message msg) {
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
			if(direction == Direction.LEFT) {
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
			collection.remove(collection.indexOf(senderId));
		}
		
		private void register() {
			InetSocketAddress sender = message.getSender();
			String clientId = "client" + ++counter;
			lock.writeLock().lock();
			collection.add(clientId, sender);
			lock.writeLock().unlock();
			RegisterResponse response = new RegisterResponse(clientId);
			endpoint.send(sender, response);
		}
	}


}
