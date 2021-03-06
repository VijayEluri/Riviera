package examples.opengl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.GLU;

import demonstration.Demonstration;
import geom.vectors.Vector3f;
import graphics.Painting;
import opengl.Frame;
import opengl.Rendering;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * Demonstrates relatively complicated transforms that generate a random world
 * populated with spheres. The camera is currently immobile, slowly rotating
 * showing the various angles of the word.
 * 
 * @author Aaron Faanes
 * 
 */
public class SphereWorldDemonstration extends GLJPanel implements GLEventListener {

	private static final long serialVersionUID = 1L;

	private Point lastPoint = null;

	/**
	 * Launches this demonstration.
	 * 
	 * @param args
	 *            unused
	 */
	public static void main(final String[] args) {
		Demonstration.launchWrapped(SphereWorldDemonstration.class);
	}

	/**
	 * Constructs a new {@link AbstractGLDemonstration}.
	 */
	public SphereWorldDemonstration() {
		this.setPreferredSize(new Dimension(800, 600));
		this.addGLEventListener(this);

		Painting.repaintPeriodically(this, 60).start();

		this.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(final MouseEvent e) {
				SphereWorldDemonstration.this.lastPoint = e.getPoint();
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				SphereWorldDemonstration.this.lastPoint = null;
			}

		});
		this.addMouseMotionListener(new MouseMotionAdapter() {

			@Override
			public void mouseDragged(final MouseEvent e) {
				final Point last = SphereWorldDemonstration.this.lastPoint;
				if (last == null) {
					return;
				}
				// SphereWorld.this.yRot += e.getX() - last.x;
				// SphereWorld.this.xRot += e.getY() - last.y;
				SphereWorldDemonstration.this.lastPoint = e.getPoint();
			}
		});
	}

	private final Frame camera = new Frame();

	private final List<Frame> frames = new ArrayList<Frame>();

	@Override
	public void init(final GLAutoDrawable drawable) {
		final GL2 gl = drawable.getGL().getGL2();
		gl.glClearColor(0, 0, .5f, 1.0f);

		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_LINE);

		for (int i = 0; i < 50; i++) {
			final float x = ((float) Math.random()) * 40.0f - 20.0f;
			final float z = ((float) Math.random()) * 40.0f - 20.0f;
			this.frames.add(new Frame(Vector3f.frozen(x, 0.0f, z)));
		}

		gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

	}

	private float zRot = 0;

	@Override
	public void display(final GLAutoDrawable drawable) {
		final GL2 gl = drawable.getGL().getGL2();

		Rendering.setClearColor(gl, Color.darkGray);
		Rendering.Buffer.COLOR.clear(gl);
		gl.glPushMatrix();
		gl.glRotatef(this.zRot, 0, 1, 0);
		this.camera.transform(gl);

		this.render(gl);

		gl.glPopMatrix();
		this.zRot += .1f;
	}

	/**
	 * Performs any rendering necessary by this demonstration.
	 * 
	 * @param gl
	 *            the rendering context
	 */
	protected void render(final GL2 gl) {
		this.drawGrid(gl);
		final GLUT glut = new GLUT();
		for (final Frame sphere : this.frames) {
			gl.glPushMatrix();
			sphere.transform(gl);
			glut.glutSolidSphere(0.1f, 13, 25);
			gl.glPopMatrix();
		}
	}

	private void drawGrid(final GL2 gl) {
		final float extent = 20.0f;
		gl.glBegin(GL.GL_LINES);
		final float y = -.4f;
		for (float line = -extent; line < extent; line += 2.0f) {
			gl.glVertex3f(line, y, -extent);
			gl.glVertex3f(line, y, extent);

			gl.glVertex3f(-extent, y, line);
			gl.glVertex3f(extent, y, line);
		}
		gl.glEnd();
	}

	@Override
	public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
		final GL2 gl = drawable.getGL().getGL2();
		final double aspectRatio = (double) width / (double) height;

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		final GLU glu = new GLU();
		glu.gluPerspective(60.0f, aspectRatio, 0, 400.0);

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		// Do nothing.
	}

}
