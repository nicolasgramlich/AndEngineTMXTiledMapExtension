package org.andengine.extension.tmx;

import java.util.ArrayList;

import org.andengine.extension.tmx.util.constants.TMXConstants;
import org.xml.sax.Attributes;

public class TMXPolygon {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================
	
	private ArrayList<TMXPolygonPoint> mPoints = new ArrayList<TMXPolygonPoint>();

	// ===========================================================
	// Constructors
	// ===========================================================

	public TMXPolygon(final Attributes pAttributes) {
		String pointsAttr = pAttributes.getValue("", TMXConstants.TAG_POLYGON_ATTRIBUTE_POINTS);
		String[] points = pointsAttr.split("\\s");
		for(String point : points) {
			mPoints.add(new TMXPolygonPoint(point));
		}
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================
	
	public ArrayList<TMXPolygonPoint> getPoints() {
		return this.mPoints;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
