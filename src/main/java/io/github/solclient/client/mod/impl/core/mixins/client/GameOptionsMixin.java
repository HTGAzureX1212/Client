/*
 * Sol Client - an open source Minecraft client
 * Copyright (C) 2021-2023  TheKodeToad and Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.solclient.client.mod.impl.core.mixins.client;

import java.io.*;
import java.util.Arrays;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.SystemUtils;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import io.github.solclient.client.util.KeyBindingInterface;
import net.minecraft.client.option.*;

@Mixin(GameOptions.class)
public class GameOptionsMixin {

	private static boolean firstLoad = true;

	@Inject(method = "<init>", at = @At("TAIL"))
	public void setDefaults(CallbackInfo callback) {
		vbo = true; // Use VBOs by default.
	}

	@Inject(method = "load", at = @At("TAIL"), cancellable = true)
	public void postLoadOptions(CallbackInfo callback) {
		if (firstLoad) {
			callback.cancel();
			firstLoad = false;
		}
	}

	// TODO this is a bad way of doing this

	@Redirect(method = "save", at = @At(value = "INVOKE", target = "Ljava/io/PrintWriter;close()V"))
	public void injectCustomOptions(PrintWriter writer) {
		for (KeyBinding keyBinding : allKeys) {
			int mods = KeyBindingInterface.from(keyBinding).getMods();
			if (mods == 0)
				continue;

			writer.print("key_mods_");
			writer.print(keyBinding.getTranslationKey());
			writer.print(':');
			writer.println(mods);
		}
		writer.close();
	}

	@Redirect(method = "load", at = @At(value = "INVOKE", target = "Ljava/io/BufferedReader;readLine()Ljava/lang/String;"))
	public String readCustomOptions(BufferedReader reader) throws IOException {
		String result = reader.readLine();
		if (result == null)
			return null;

		if (result.startsWith("key_mods_") && result.indexOf(':') != -1) {
			String key = result.substring(9, result.indexOf(':'));
			String value = result.substring(result.indexOf(':') + 1);
			StreamSupport.stream(Arrays.spliterator(allKeys), false)
					.filter((binding) -> binding.getTranslationKey().equals(key)).findFirst()
					.ifPresent((binding) -> KeyBindingInterface.from(binding).setMods(Integer.parseInt(value)));
		}

		return result;
	}

	@Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
	private static void requireMods(KeyBinding key, CallbackInfoReturnable<Boolean> callback) {
		if (callback.getReturnValueZ() && !KeyBindingInterface.from(key).areModsPressed())
			callback.setReturnValue(false);
	}

	@Inject(method = "shouldUseNativeTransport", at = @At("RETURN"), cancellable = true)
	public void ilikemovingbetweenplacesbutthishastogoimsorryitsverysadiguessilljusthavetousenonnativetransport(CallbackInfoReturnable<Boolean> callback) {
		callback.setReturnValue(callback.getReturnValueZ() && SUPER_OLD_BORING_JAVA_VERSION);
	}

	private static final boolean SUPER_OLD_BORING_JAVA_VERSION = SystemUtils.JAVA_SPECIFICATION_VERSION.startsWith("1.");

	@Shadow
	public boolean vbo;

	@Shadow
	public KeyBinding[] allKeys;

}
