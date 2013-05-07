package org.andengine.extension.tmx;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.sprite.batch.SpriteBatch;
import org.andengine.extension.tmx.TMXLoader.ITMXTilePropertiesListener;
import org.andengine.extension.tmx.util.constants.TMXConstants;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.util.GLState;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.SAXUtils;
import org.andengine.util.StreamUtils;
import org.andengine.util.adt.color.Color;
import org.andengine.util.exception.AndEngineRuntimeException;
import org.andengine.util.exception.MethodNotSupportedException;
import org.andengine.util.math.MathUtils;
import org.xml.sax.Attributes;

import android.opengl.GLES20;
import android.util.Base64;
import android.util.Base64InputStream;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 20:27:31 - 20.07.2010
 */
public class TMXLayer extends SpriteBatch implements TMXConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private final TMXTiledMap mTMXTiledMap;

	private final String mName;
	private final int mTileColumns;
	private final int mTileRows;
	private final TMXTile[][] mTMXTiles;

	private int mTilesAdded;
	private final int mGlobalTileIDsExpected;

	private final TMXProperties<TMXLayerProperty> mTMXLayerProperties = new TMXProperties<TMXLayerProperty>();

	// ===========================================================
	// Constructors
	// ===========================================================

	public TMXLayer(final TMXTiledMap pTMXTiledMap, final Attributes pAttributes, final VertexBufferObjectManager pVertexBufferObjectManager) {
		super(null, SAXUtils.getIntAttributeOrThrow(pAttributes, TMXConstants.TAG_LAYER_ATTRIBUTE_WIDTH) * SAXUtils.getIntAttributeOrThrow(pAttributes, TMXConstants.TAG_LAYER_ATTRIBUTE_HEIGHT), pVertexBufferObjectManager);

		this.mTMXTiledMap = pTMXTiledMap;
		this.mName = pAttributes.getValue("", TMXConstants.TAG_LAYER_ATTRIBUTE_NAME);
		this.mTileColumns = SAXUtils.getIntAttributeOrThrow(pAttributes, TMXConstants.TAG_LAYER_ATTRIBUTE_WIDTH);
		this.mTileRows = SAXUtils.getIntAttributeOrThrow(pAttributes, TMXConstants.TAG_LAYER_ATTRIBUTE_HEIGHT);
		this.mTMXTiles = new TMXTile[this.mTileRows][this.mTileColumns];

		final int width = pTMXTiledMap.getTileWidth() * this.mTileColumns;
		final int height = pTMXTiledMap.getTileHeight() * this.mTileRows;

		this.setPosition(width / 2, height / 2);
		this.setSize(width, height);

		this.mGlobalTileIDsExpected = this.mTileColumns * this.mTileRows;

		this.setVisible(SAXUtils.getIntAttribute(pAttributes, TMXConstants.TAG_LAYER_ATTRIBUTE_VISIBLE, TMXConstants.TAG_LAYER_ATTRIBUTE_VISIBLE_VALUE_DEFAULT) == 1);
		this.setAlpha(SAXUtils.getFloatAttribute(pAttributes, TMXConstants.TAG_LAYER_ATTRIBUTE_OPACITY, TMXConstants.TAG_LAYER_ATTRIBUTE_OPACITY_VALUE_DEFAULT));

		this.setCullingEnabled(true);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public String getName() {
		return this.mName;
	}

	public int getTileX(final int pTileColumn) {
		return pTileColumn * this.mTMXTiledMap.getTileWidth();
	}

	public int getTileY(final int pTileRow) {
		return (this.mTMXTiledMap.getTileRows() - pTileRow - 1) * this.mTMXTiledMap.getTileHeight();
	}

	public int getTileColumns() {
		return this.mTileColumns;
	}

	public int getTileRows() {
		return this.mTileRows;
	}

	private int getTileWidth() {
		return this.mTMXTiledMap.getTileWidth();
	}

	private int getTileHeight() {
		return this.mTMXTiledMap.getTileHeight();
	}

	public TMXTile[][] getTMXTiles() {
		return this.mTMXTiles;
	}

	public TMXTile getTMXTile(final int pTileColumn, final int pTileRow) throws ArrayIndexOutOfBoundsException {
		return this.getTMXTile(pTileColumn, pTileRow, false);
	}

	public TMXTile getTMXTile(final int pTileColumn, final int pTileRow, final boolean pReturnClosestTMXTileIfOutOfBounds) throws ArrayIndexOutOfBoundsException {
		if(pReturnClosestTMXTileIfOutOfBounds) {
			final int tileRow = MathUtils.bringToBounds(0, this.mTileRows - 1, pTileRow);
			final int tileColumn = MathUtils.bringToBounds(0, this.mTileColumns - 1, pTileColumn);

			return this.mTMXTiles[tileRow][tileColumn];
		} else {
			if((pTileRow < 0) || (pTileRow > (this.mTileRows - 1)) || (pTileColumn < 0) || (pTileColumn > (this.mTileColumns - 1))) {
				return null;
			} else {
				return this.mTMXTiles[pTileRow][pTileColumn];
			}
		}
	}

	public TMXTile getTMXTileAt(final float pSceneX, final float pSceneY) {
		return this.getTMXTileAt(pSceneX, pSceneY, false);
	}

	public TMXTile getTMXTileAtLocal(final float pSceneX, final float pSceneY) {
		return this.getTMXTileAtLocal(pSceneX, pSceneY, false);
	}

	public TMXTile getTMXTileAt(final float pSceneX, final float pSceneY, final boolean pReturnClosestTMXTileIfOutOfBounds) {
		final float[] localCoordinates = this.convertSceneCoordinatesToLocalCoordinates(pSceneX, pSceneY);

		final float localX = localCoordinates[SpriteBatch.VERTEX_INDEX_X];
		final float localY = localCoordinates[SpriteBatch.VERTEX_INDEX_Y];

		return this.getTMXTileAtLocal(localX, localY, pReturnClosestTMXTileIfOutOfBounds);
	}

	public TMXTile getTMXTileAtLocal(final float pLocalX, final float pLocalY, final boolean pReturnClosestTMXTileIfOutOfBounds) {
		final int tileColumn = this.getTileColumnFromLocalX(pLocalX);
		final int tileRow = this.getTileRowFromLocalY(pLocalY);

		return this.getTMXTile(tileColumn, tileRow, pReturnClosestTMXTileIfOutOfBounds);
	}

	public void addTMXLayerProperty(final TMXLayerProperty pTMXLayerProperty) {
		this.mTMXLayerProperties.add(pTMXLayerProperty);
	}

	public TMXProperties<TMXLayerProperty> getTMXLayerProperties() {
		return this.mTMXLayerProperties;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void initBlendFunction(final ITexture pTexture) {

	}

	@Override
	@Deprecated
	public void setRotation(final float pRotation) throws MethodNotSupportedException {
		throw new MethodNotSupportedException();
	}

	@Override
	protected void onManagedUpdate(final float pSecondsElapsed) {
		/* Nothing. */
	}

	@Override
	public boolean isCulled(final Camera pCamera) {
		final float cameraXMin = pCamera.getXMin();
		final float cameraYMin = pCamera.getYMin();
		final float[] cameraLocalCoordinatesMin = this.convertSceneCoordinatesToLocalCoordinates(cameraXMin, cameraYMin);

		final float cameraLocalXMin = cameraLocalCoordinatesMin[SpriteBatch.VERTEX_INDEX_X];
		final float cameraLocalYMin = cameraLocalCoordinatesMin[SpriteBatch.VERTEX_INDEX_Y];

		if((cameraLocalXMin > this.mWidth) || (cameraLocalYMin > this.mHeight)) {
			return true;
		}

		final float cameraXMax = pCamera.getXMax();
		final float cameraYMax = pCamera.getYMax();
		final float[] localCoordinatesMax = this.convertSceneCoordinatesToLocalCoordinates(cameraXMax, cameraYMax);

		final float cameraLocalXMax = localCoordinatesMax[SpriteBatch.VERTEX_INDEX_X];
		final float cameraLocalYMax = localCoordinatesMax[SpriteBatch.VERTEX_INDEX_Y];
		if((cameraLocalXMax < 0) || (cameraLocalYMax < 0)) {
			return true;
		}

		return false;
	}

	@Override
	protected void draw(final GLState pGLState, final Camera pCamera) {
		final float cameraXMin = pCamera.getXMin();
		final float cameraYMin = pCamera.getYMin();
		final float cameraXMax = pCamera.getXMax();
		final float cameraYMax = pCamera.getYMax();

		final TMXTile tmxTileMin = this.getTMXTileAt(cameraXMin, cameraYMin, true);
		final TMXTile tmxTileMax = this.getTMXTileAt(cameraXMax, cameraYMax, true);

		/* TMX rows are counted up, starting from the topmost row to the bottom row. */
		final int tileColumnMin = tmxTileMin.getTileColumn();
		final int tileRowMin = tmxTileMax.getTileRow();
		final int tileColumnMax = tmxTileMax.getTileColumn();
		final int tileRowMax = tmxTileMin.getTileRow();

		final int rowLength = (tileColumnMax - tileColumnMin) + 1;

		for(int row = tileRowMin; row <= tileRowMax; row++) {
			final int spriteBatchIndex = this.getSpriteBatchIndex(tileColumnMin, row);
			this.mSpriteBatchVertexBufferObject.draw(GLES20.GL_TRIANGLES, spriteBatchIndex * SpriteBatch.VERTICES_PER_SPRITE, rowLength * SpriteBatch.VERTICES_PER_SPRITE);
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	/* package */ void initializeTMXTileFromXML(final Attributes pAttributes, final ITMXTilePropertiesListener pTMXTilePropertyListener) {
		this.addTileByGlobalTileID(SAXUtils.getIntAttributeOrThrow(pAttributes, TMXConstants.TAG_TILE_ATTRIBUTE_GID), pTMXTilePropertyListener);
	}

	/* package */ void initializeTMXTilesFromDataString(final String pDataString, final String pDataEncoding, final String pDataCompression, final ITMXTilePropertiesListener pTMXTilePropertyListener) throws IOException, IllegalArgumentException {
		DataInputStream dataIn = null;
		try{
			InputStream in = new ByteArrayInputStream(pDataString.getBytes("UTF-8"));

			/* Wrap decoding Streams if necessary. */
			if((pDataEncoding != null) && pDataEncoding.equals(TMXConstants.TAG_DATA_ATTRIBUTE_ENCODING_VALUE_BASE64)) {
				in = new Base64InputStream(in, Base64.DEFAULT);
			}
			if(pDataCompression != null){
				if(pDataCompression.equals(TMXConstants.TAG_DATA_ATTRIBUTE_COMPRESSION_VALUE_GZIP)) {
					in = new GZIPInputStream(in);
				} else if(pDataCompression.equals(TMXConstants.TAG_DATA_ATTRIBUTE_COMPRESSION_VALUE_ZLIB)){
					in = new InflaterInputStream(in, new Inflater());
				} else {
					throw new IllegalArgumentException("Supplied compression '" + pDataCompression + "' is not supported yet.");
				}
			}
			dataIn = new DataInputStream(in);

			while(this.mTilesAdded < this.mGlobalTileIDsExpected) {
				final int globalTileID = this.readGlobalTileID(dataIn);
				this.addTileByGlobalTileID(globalTileID, pTMXTilePropertyListener);
			}
			this.submit();
		} finally {
			StreamUtils.close(dataIn);
		}
	}

	private void addTileByGlobalTileID(final int pGlobalTileID, final ITMXTilePropertiesListener pTMXTilePropertyListener) {
		final TMXTiledMap tmxTiledMap = this.mTMXTiledMap;

		final int tileColumn = this.mTilesAdded % this.mTileColumns;
		final int tileRow = this.mTilesAdded / this.mTileColumns;

		final TMXTile[][] tmxTiles = this.mTMXTiles;

		final int tileHeight = this.getTileHeight();
		final int tileWidth = this.getTileWidth();
		if(pGlobalTileID == 0) {
			final TMXTile tmxTile = new TMXTile(pGlobalTileID, tileColumn, tileRow, tileWidth, tileHeight, null);
			tmxTiles[tileRow][tileColumn] = tmxTile;
		} else {
			final ITextureRegion tmxTileTextureRegion = tmxTiledMap.getTextureRegionFromGlobalTileID(pGlobalTileID);

			if(this.mTexture == null) {
				this.mTexture = tmxTileTextureRegion.getTexture();
				super.initBlendFunction(this.mTexture);
			} else {
				if(this.mTexture != tmxTileTextureRegion.getTexture()) {
					throw new AndEngineRuntimeException("All TMXTiles in a TMXLayer need to be in the same TMXTileSet.");
				}
			}
			final TMXTile tmxTile = new TMXTile(pGlobalTileID, tileColumn, tileRow, tileWidth, tileHeight, tmxTileTextureRegion);
			tmxTiles[tileRow][tileColumn] = tmxTile;

			this.setIndex(this.getSpriteBatchIndex(tileColumn, tileRow));
			final float tileX = this.getTileX(tileColumn);
			final float tileY = this.getTileY(tileRow);
			this.drawWithoutChecks(tmxTileTextureRegion, tileX, tileY, tileWidth, tileHeight, Color.WHITE_ABGR_PACKED_FLOAT);

			/* Notify the ITMXTilePropertiesListener if it exists. */
			if(pTMXTilePropertyListener != null) {
				final TMXProperties<TMXTileProperty> tmxTileProperties = tmxTiledMap.getTMXTileProperties(pGlobalTileID);
				if(tmxTileProperties != null) {
					pTMXTilePropertyListener.onTMXTileWithPropertiesCreated(tmxTiledMap, this, tmxTile, tmxTileProperties);
				}
			}
		}

		this.mTilesAdded++;
	}

	private int getTileRowFromLocalY(final float pLocalY) {
		return this.mTMXTiledMap.getTileRows() - this.getTileColumnFromLocalX(pLocalY) - 1;
	}

	private int getTileColumnFromLocalX(final float pLocalX) {
		return (int)(pLocalX / this.mTMXTiledMap.getTileWidth());
	}

	private int getSpriteBatchIndex(final int pTileColumn, final int pTileRow) {
		return (pTileRow * this.mTileColumns) + pTileColumn;
	}

	private int readGlobalTileID(final DataInputStream pDataIn) throws IOException {
		final int lowestByte = pDataIn.read();
		final int secondLowestByte = pDataIn.read();
		final int secondHighestByte = pDataIn.read();
		final int highestByte = pDataIn.read();

		if((lowestByte < 0) || (secondLowestByte < 0) || (secondHighestByte < 0) || (highestByte < 0)) {
			throw new IllegalArgumentException("Couldn't read global Tile ID.");
		}

		return lowestByte | (secondLowestByte <<  8) |(secondHighestByte << 16) | (highestByte << 24);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
