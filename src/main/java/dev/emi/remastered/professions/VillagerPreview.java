package dev.emi.remastered.professions;

import org.joml.Quaternionf;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.emi.emi.runtime.EmiLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.village.VillagerProfession;

/**
 * Renders a shared, never spawned villager entity wearing a given profession.
 */
public class VillagerPreview {
	private static VillagerEntity villager;
	private static boolean failed;

	/**
	 * Drops the cached entity, it belongs to the client world it was created in.
	 */
	public static void clear() {
		villager = null;
	}

	private static VillagerEntity get(VillagerProfession profession) {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null) {
			return null;
		}
		if (villager == null || villager.getWorld() != world) {
			villager = EntityType.VILLAGER.create(world);
			if (villager == null) {
				return null;
			}
		}
		villager.setVillagerData(villager.getVillagerData().withProfession(profession));
		return villager;
	}

	/**
	 * @param x     x of the feet
	 * @param y     y of the feet
	 * @param scale Scale of the entity
	 * @param lookX Horizontal distance to the point the villager looks at
	 * @param lookY Vertical distance to the point the villager looks at
	 */
	public static void render(DrawContext context, int x, int y, double scale, double lookX, double lookY, VillagerProfession profession) {
		if (failed || !ProfessionsSettings.showVillagerModel) {
			return;
		}
		VillagerEntity villager = get(profession);
		if (villager == null) {
			return;
		}
		MatrixStack matrices = context.getMatrices();
		matrices.push();
		EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
		try {
			matrices.translate((float) x, (float) y, 50f);
			matrices.scale((float) scale, (float) scale, (float) scale);
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(((float) Math.atan(-1.0)) * 10.0F));

			float yaw = (float) -(lookX / 40.0) * 20.0F;
			villager.bodyYaw = yaw;
			villager.setYaw(yaw);
			villager.headYaw = yaw;
			villager.prevHeadYaw = yaw;
			villager.setPitch((float) -(lookY / 5.0));

			matrices.translate(0.0F, villager.getVehicleAttachmentPos(villager).getY(), 0.0F);
			dispatcher.setRotation(new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F));
			dispatcher.setRenderShadows(false);
			VertexConsumerProvider.Immediate buffers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
			RenderSystem.runAsFancy(() -> dispatcher.render(villager, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, matrices, buffers, 15728880));
			buffers.draw();
		} catch (RuntimeException e) {
			failed = true;
			EmiLog.error("[Professions] Villager preview failed for " + profession + ", disabling it: " + e);
		} finally {
			dispatcher.setRenderShadows(true);
			matrices.pop();
		}
	}
}
