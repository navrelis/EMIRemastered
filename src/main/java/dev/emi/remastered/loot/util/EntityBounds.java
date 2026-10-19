package dev.emi.remastered.loot.util;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import dev.emi.remastered.loot.EMILoot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

/**
 * Measures how large an entity really renders: the model is drawn into a consumer that only records the corners of
 * the vertices. The hit box says little about it (a bat model is drawn at a third of its box). Client only.
 */
public final class EntityBounds {
	private EntityBounds() {
	}

	/**
	 * The extent of the rendered entity in the screen space of its portrait: x to the right, y down, one unit is one
	 * block, the origin is the point the entity is rendered at (its feet, centred).
	 */
	public record Box(float minX, float minY, float maxX, float maxY, float minZ, float maxZ) {
		public float depth() {
			return maxZ - minZ;
		}

		public float width() {
			return maxX - minX;
		}

		public float height() {
			return maxY - minY;
		}
	}

	/**
	 * @param transform the extra rotation of the entity in radians (from the loot module's entity fixers), null for none
	 * @return null when nothing was drawn or the entity could not be measured
	 */
	@Nullable
	public static Box measure(LivingEntity entity, @Nullable Vector3f transform) {
		EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
		Capture capture = new Capture();
		float bodyYaw = entity.bodyYaw;
		float yaw = entity.getYaw();
		float pitch = entity.getPitch();
		float prevHeadYaw = entity.prevHeadYaw;
		float headYaw = entity.headYaw;
		try {
			// the pose of the portrait when the mouse is in the middle of the screen
			entity.bodyYaw = 180.0F;
			entity.setYaw(180.0F);
			entity.setPitch(0.0F);
			entity.headYaw = 180.0F;
			entity.prevHeadYaw = 180.0F;
			MatrixStack matrices = new MatrixStack();
			Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
			if (transform != null) {
				rotation.mul(new Quaternionf().rotateXYZ(transform.x, transform.y, transform.z));
			}
			matrices.multiply(rotation);
			dispatcher.setRenderShadows(false);
			dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0F, 1.0F, matrices, capture, 15728880);
		} catch (RuntimeException e) {
			EMILoot.LOGGER.debug("Could not measure {} for its portrait", entity.getType(), e);
			return null;
		} finally {
			dispatcher.setRenderShadows(true);
			entity.bodyYaw = bodyYaw;
			entity.setYaw(yaw);
			entity.setPitch(pitch);
			entity.headYaw = headYaw;
			entity.prevHeadYaw = prevHeadYaw;
		}
		return capture.box();
	}

	private static final class Capture implements VertexConsumer, VertexConsumerProvider {
		private float minX = Float.POSITIVE_INFINITY;
		private float minY = Float.POSITIVE_INFINITY;
		private float maxX = Float.NEGATIVE_INFINITY;
		private float maxY = Float.NEGATIVE_INFINITY;
		private float minZ = Float.POSITIVE_INFINITY;
		private float maxZ = Float.NEGATIVE_INFINITY;

		@Nullable
		Box box() {
			if (minX > maxX || minY > maxY) {
				return null;
			}
			return new Box(minX, minY, maxX, maxY, minZ, maxZ);
		}

		@Override
		public VertexConsumer getBuffer(RenderLayer layer) {
			return this;
		}

		@Override
		public VertexConsumer vertex(float x, float y, float z) {
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
			minZ = Math.min(minZ, z);
			maxZ = Math.max(maxZ, z);
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			return this;
		}

		@Override
		public VertexConsumer texture(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer overlay(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer light(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			return this;
		}
	}
}
