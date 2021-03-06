package impl;

import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import org.piccolo2d.PCamera;
import org.piccolo2d.PCanvas;
import org.piccolo2d.event.PBasicInputEventHandler;
import org.piccolo2d.event.PInputEvent;
import org.piccolo2d.event.PInputEventFilter;

public class PMouseWheelEventHandler extends PBasicInputEventHandler {

    /** Default scale factor, <code>0.1d</code>. */
    static final double DEFAULT_SCALE_FACTOR = 0.1d;

    /** Scale factor. */
    private double scaleFactor = DEFAULT_SCALE_FACTOR;

    /** Zoom mode. */
    private ZoomMode zoomMode = ZoomMode.ZOOM_ABOUT_CANVAS_CENTER;
    
    Frame frame;
    PCanvas canvas;
    
    double height;
    double width;
    
    int counterZoomOut = 0;
	
	 /**
     * Creates a new zoom handler.
	 * @param frame 
	 * @param pSwingNode 
	 * @param canvas 
     */
    public PMouseWheelEventHandler() {
        super();
//        this.frame = frame;
        
        PInputEventFilter eventFilter = new PInputEventFilter();
        eventFilter.rejectAllEventTypes();
        eventFilter.setAcceptsMouseWheelRotated(true);
        setEventFilter(eventFilter);
    }
    
    
    /**
     * Return the scale factor for this mouse wheel zoom event handler.  Defaults to <code>DEFAULT_SCALE_FACTOR</code>.
     *
     * @see #DEFAULT_SCALE_FACTOR
     * @return the scale factor for this mouse wheel zoom event handler
     */
    public double getScaleFactor() {
        return scaleFactor;
    }

    /**
     * Set the scale factor for this mouse wheel zoom event handler to <code>scaleFactor</code>.
     *
     * @param scaleFactor scale factor for this mouse wheel zoom event handler
     */
    public void setScaleFactor(final double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    /**
     * Switch to zoom about mouse mode.
     *
     * @see ZoomMode#ZOOM_ABOUT_MOUSE
     */
    public void zoomAboutMouse() {
        zoomMode = ZoomMode.ZOOM_ABOUT_MOUSE;
    }

    /**
     * Switch to zoom about canvas center mode.
     *
     * @see ZoomMode#ZOOM_ABOUT_CANVAS_CENTER
     */
    public void zoomAboutCanvasCenter() {
        zoomMode = ZoomMode.ZOOM_ABOUT_CANVAS_CENTER;
    }

    /**
     * Switch to zoom about view center mode.
     *
     * @see ZoomMode#ZOOM_ABOUT_VIEW_CENTER
     */
    public void zoomAboutViewCenter() {
        zoomMode = ZoomMode.ZOOM_ABOUT_VIEW_CENTER;
    }

    /**
     * Return the zoom mode for this mouse wheel zoom event handler.  Defaults to
     * <code>ZoomMode.ZOOM_ABOUT_CANVAS_CENTER</code>.
     *
     * @return the zoom mode for this mouse wheel zoom event handler
     */
    ZoomMode getZoomMode() {
        return zoomMode;
    }

    /** {@inheritDoc} */
    public void mouseWheelRotated(final PInputEvent event) {
        PCamera camera = event.getCamera();
        double scale = 1.0d + event.getWheelRotation() * scaleFactor;
        
        Point2D viewAboutPoint = getViewAboutPoint(event);
        camera.scaleViewAboutPoint(scale, viewAboutPoint.getX(), viewAboutPoint.getY());
        
        System.out.println(">> ZC: "+counterZoomOut +"  >> ViewScale: "+camera.getViewScale());
        
       
    }

    /**
     * Return the view about point for the specified event according to the current zoom mode.
     *
     * @param event input event
     * @return the view about point for the specified event according to the current zoom mode
     */
    private Point2D getViewAboutPoint(final PInputEvent event) {
        switch (zoomMode) {
            case ZOOM_ABOUT_MOUSE:
                return event.getPosition();
            case ZOOM_ABOUT_CANVAS_CENTER:
                Rectangle canvasBounds = ((PCanvas) event.getComponent()).getBounds();
                Point2D canvasCenter = new Point2D.Double(canvasBounds.getCenterX(), canvasBounds.getCenterY());
                event.getPath().canvasToLocal(canvasCenter, event.getCamera());
                return event.getCamera().localToView(canvasCenter);
            case ZOOM_ABOUT_VIEW_CENTER:
                return event.getCamera().getBoundsReference().getCenter2D();
        }
        throw new IllegalArgumentException("illegal zoom mode " + zoomMode);
    }

    /**
     * Zoom mode.
     */
    enum ZoomMode {
        /**
         * Zoom about mouse mode.
         */
        ZOOM_ABOUT_MOUSE,

        /**
         * Zoom about canvas center mode.
         */
        ZOOM_ABOUT_CANVAS_CENTER,

        /**
         * Zoom about view center mode.
         */
        ZOOM_ABOUT_VIEW_CENTER;
    }
	
}
