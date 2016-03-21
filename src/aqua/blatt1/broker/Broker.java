package aqua.blatt1.broker;

import java.net.InetSocketAddress;

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
	
	public static void main(final String[] args) {
		Broker broker = new Broker();
		broker.broker();
	}

	private void broker() {
		while (true) {
			Message msg = endpoint.blockingReceive();
			if (msg.getPayload() instanceof RegisterRequest) {
				register(msg);
			} else if (msg.getPayload() instanceof DeregisterRequest) {
				deregister((DeregisterRequest) msg.getPayload());
			} else if (msg.getPayload() instanceof HandoffRequest) {
				handofffish(msg);
			}
		}
	}

	private void handofffish(Message msg) {
		HandoffRequest request = (HandoffRequest) msg.getPayload();
		FishModel fish = request.getFish();
		Direction direction = fish.getDirection();
		int index = collection.indexOf(msg.getSender());
		InetSocketAddress receiver = null;
		if(direction == Direction.LEFT) {
			receiver = collection.getLeftNeighorOf(index);
		} else {
			receiver = collection.getRightNeighorOf(index);
		}
		endpoint.send(receiver, request);
	}

	private void deregister(DeregisterRequest payload) {
		String senderId = payload.getId();
		collection.remove(collection.indexOf(senderId));
	}

	private void register(Message msg) {
		InetSocketAddress sender = msg.getSender();
		String clientId = "client" + ++counter;
		collection.add(clientId, sender);
		RegisterResponse response = new RegisterResponse(clientId);
		endpoint.send(sender, response);
	}

}
