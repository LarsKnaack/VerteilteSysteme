package aqua.client;

import aqua.common.FishModel;
import aqua.common.Properties;
import aqua.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;

import java.net.InetSocketAddress;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish, InetSocketAddress receiver) {
			endpoint.send(broker, new HandoffRequest(fish));
		}

		public void sendToken(InetSocketAddress leftNeighbour, Token token) {
			endpoint.send(leftNeighbour, token);
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());
				if (msg.getPayload() instanceof NeighbourUpdate) {
					tankModel.updateNeighbours((NeighbourUpdate) msg.getPayload());
				}
				if (msg.getPayload() instanceof Token) {
					tankModel.recieveToken((Token) msg.getPayload());
				}
			}
			System.out.println("Receiver stopped.");
		}
	}

}
