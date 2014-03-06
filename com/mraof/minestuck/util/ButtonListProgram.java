package com.mraof.minestuck.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.StatCollector;

import com.mraof.minestuck.client.gui.GuiComputer;
import com.mraof.minestuck.network.ClearMessagePacket;
import com.mraof.minestuck.network.skaianet.ComputerData;
import com.mraof.minestuck.tileentity.TileEntityComputer;

public abstract class ButtonListProgram extends ComputerProgram {
	
	private LinkedHashMap<GuiButton,UnlocalizedString> buttonMap = new LinkedHashMap();
	private GuiButton upButton, downButton;
	private String message;
	
	private int index = 0;
	
	protected abstract ArrayList<UnlocalizedString> getStringList(TileEntityComputer te);
	
	protected abstract void onButtonPressed(TileEntityComputer te, String buttonName, Object[] data);
	
	@Override
	public final void onButtonPressed(TileEntityComputer te, GuiButton button) {
		UnlocalizedString data = buttonMap.get(button);
		if (button == upButton)
			index--;
		else if (button == downButton)
			index++;
		else if(data != null) {
			if(!te.latestmessage.get(this.getId()).isEmpty())
				ClearMessagePacket.send(ComputerData.createData(te), this.getId());
			onButtonPressed(te, data.string, data.formatData);
		}
	}
	
	@Override
	public final void onInitGui(GuiComputer gui, List buttonList, ComputerProgram prevProgram) {
		if(prevProgram instanceof ButtonListProgram) {
			ButtonListProgram program = (ButtonListProgram) prevProgram;
			buttonMap = program.buttonMap;
			downButton = program.downButton;
			upButton = program.upButton;
		} else {
			if(prevProgram != null) {
				buttonList.clear();
				buttonList.add(gui.programButton);
			}
			buttonMap.clear();
			for (int i = 0; i < 4; i++) {
				GuiButton button = new GuiButton(i+2, (gui.width - gui.xSize) / 2 +14, (gui.height - gui.ySize) / 2 +60 + i*24, 120, 20,"");
				buttonMap.put(button, new UnlocalizedString(""));
				buttonList.add(button);
			}
			
			upButton = new GuiButton(-1, (gui.width - gui.xSize) / 2 +140, (gui.height - gui.ySize) / 2 +60, 20, 20,"^");
			buttonList.add(upButton);
			downButton = new GuiButton(-1, (gui.width - gui.xSize) / 2 +140, (gui.height - gui.ySize) / 2 +132, 20, 20,"v");
			buttonList.add(downButton);
		}
	}
	
	@Override
	public final void onUpdateGui(GuiComputer gui, List buttonList) {
		downButton.enabled = false;
		upButton.enabled = index > 0;
		ArrayList<UnlocalizedString> list = getStringList(gui.te);
		if(!gui.te.latestmessage.get(this.getId()).isEmpty())
			list.add(1, new UnlocalizedString("computer.buttonClear"));
		int pos = -1;
		for(UnlocalizedString s : list) {
			if(pos == -1) {
				message = s.translate();
			} else {
				if(index > pos) {
					pos++;
					continue;
				}
				if(pos == index+4) {
					downButton.enabled = true;
					break;
				}
				buttonMap.put((GuiButton) buttonMap.keySet().toArray()[pos-index], s);
			}
			pos++;
		}
		if(index == 0 && pos != 4)
			for(; pos < 4; pos++)
				buttonMap.put((GuiButton) buttonMap.keySet().toArray()[pos-index], new UnlocalizedString(""));
		
		for(Entry<GuiButton, UnlocalizedString> entry : buttonMap.entrySet()) {
			UnlocalizedString data = entry.getValue();
			entry.getKey().enabled = !data.string.isEmpty();
			entry.getKey().displayString = data.translate();
		}
	}
	
	@Override
	public final void paintGui(GuiComputer gui, TileEntityComputer te) {
		Minecraft mc = Minecraft.getMinecraft();
		mc.getTextureManager().bindTexture(GuiComputer.guiBackground);
		int yOffset = (gui.height / 2) - (gui.ySize / 2);
		gui.drawTexturedModalRect((gui.width / 2) - (gui.xSize / 2), yOffset, 0, 0, gui.xSize, gui.ySize);
		if(te.latestmessage.get(te.programSelected) == null || te.latestmessage.get(te.programSelected).isEmpty())
			mc.fontRenderer.drawString(message, (gui.width - gui.xSize) / 2 +15, (gui.height - gui.ySize) / 2 +45, 4210752);
		else mc.fontRenderer.drawString(StatCollector.translateToLocal(te.latestmessage.get(te.programSelected)), (gui.width - gui.xSize) / 2 +15, (gui.height - gui.ySize) / 2 +45, 4210752);
	}
	
	protected static class UnlocalizedString {
		String string;
		Object[] formatData;
		public UnlocalizedString(String str, Object... obj) {
			string = str;
			formatData = obj;
		}
		
		public String translate() {
			return StatCollector.translateToLocalFormatted(string, formatData);
		}
		
	}
	
}