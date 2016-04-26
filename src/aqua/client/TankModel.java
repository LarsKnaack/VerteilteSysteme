package aqua.client;

import aqua.common.Direction;
import aqua.common.FishModel;
import aqua.common.msgtypes.NeighbourUpdate;
import aqua.common.msgtypes.Token;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected final Set<FishModel> fishies;
	protected final ClientCommunicator.ClientForwarder forwarder;
	protected volatile String id;
	protected int fishCounter = 0;
	protected InetSocketAddress leftNeighbour;
	protected InetSocketAddress rightNeighbour;
	protected boolean token;
	protected Timer timer;

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
		this.timer = new Timer();
		this.token = false;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
		}
	}

	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);
	}

	synchronized void recieveToken(Token msg) {
		this.token = true;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				token = false;
				forwarder.sendToken(leftNeighbour, msg);
			}
		}, 500);
	}

	public String getId() {
		return id;
	}

	public boolean hasToken() {
		return token;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge()) {
				if (fish.getDirection() == Direction.LEFT) {
					if (token) {
						forwarder.handOff(fish, leftNeighbour);
					} else {
						fish.reverse();
					}
				} else {
					if (token) {
						forwarder.handOff(fish, rightNeighbour);
					} else {
						fish.reverse();
					}
				}
			}

			if (fish.disappears())
				it.remove();
		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}

	public void updateNeighbours(NeighbourUpdate payload) {
		if (payload.getLeft() != null) {
			leftNeighbour = payload.getLeft();
		} else if (payload.getRight() != null) {
			rightNeighbour = payload.getRight();
		}
	}
}