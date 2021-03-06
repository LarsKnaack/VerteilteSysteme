package aqua.client;

import aqua.common.FishModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.Observable;
import java.util.Observer;

@SuppressWarnings("serial")
public class TankView extends JPanel implements Observer {
	private final TankModel tankModel;
	private final FishView fishView;
	private final Runnable repaintRunnable;

	public TankView(final TankModel tankModel) {
		this.tankModel = tankModel;
		fishView = new FishView();

		repaintRunnable = new Runnable() {
			@Override
			public void run() {
				repaint();
			}
		};

		setPreferredSize(new Dimension(TankModel.WIDTH, TankModel.HEIGHT));
		setBackground(new Color(175, 200, 235));

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				tankModel.newFish(e.getX(), e.getY());
			}
		});
	}

	@SuppressWarnings("unused")
	private void drawBorders(Graphics2D g2d) {
		g2d.draw(new Line2D.Double(1, 0, 1, TankModel.HEIGHT));
		g2d.draw(new Line2D.Double(TankModel.WIDTH, 0, TankModel.WIDTH, TankModel.HEIGHT));
	}

	private void doDrawing(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;

		for (FishModel fishModel : tankModel) {
			g2d.drawImage(fishView.getImage(fishModel), fishModel.getX(), fishModel.getY(), null);
			g2d.drawString(fishModel.getId(), fishModel.getX(), fishModel.getY());
		}

	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		doDrawing(g);
		if (!tankModel.hasToken()) {
			drawBorders((Graphics2D) g);
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		SwingUtilities.invokeLater(repaintRunnable);
	}
}
