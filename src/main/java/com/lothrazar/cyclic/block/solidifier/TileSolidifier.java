package com.lothrazar.cyclic.block.solidifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.lothrazar.cyclic.base.FluidTankBase;
import com.lothrazar.cyclic.base.TileEntityBase;
import com.lothrazar.cyclic.capability.CustomEnergyStorage;
import com.lothrazar.cyclic.capability.ItemStackHandlerSided;
import com.lothrazar.cyclic.data.Const;
import com.lothrazar.cyclic.registry.TileRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

@SuppressWarnings("rawtypes")
public class TileSolidifier extends TileEntityBase implements ITickableTileEntity, INamedContainerProvider {

  public final static int TIMER_FULL = Const.TICKS_PER_SEC * 5;
  public static final int MAX = 64000;
  public static final int CAPACITY = 64 * FluidAttributes.BUCKET_VOLUME;
  public static final int TRANSFER_FLUID_PER_TICK = FluidAttributes.BUCKET_VOLUME / 20;
  public static IntValue POWERCONF;
  private RecipeSolidifier currentRecipe;
  FluidTankBase tank;
  private ItemStackHandlerSided inputSlots = new ItemStackHandlerSided(3);
  private ItemStackHandlerSided outputSlot = new ItemStackHandlerSided(1);
  CustomEnergyStorage energy = new CustomEnergyStorage(MAX, MAX);
  private final LazyOptional<FluidTankBase> tankWrapper = LazyOptional.of(() -> tank);
  private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);
  private final LazyOptional<IItemHandler> inventoryCap = LazyOptional.of(() -> inputSlots);
  private final LazyOptional<IItemHandler> outputSlotWrapper = LazyOptional.of(() -> outputSlot);

  static enum Fields {
    REDSTONE, TIMER, RENDER;
  }

  public TileSolidifier() {
    super(TileRegistry.solidifier);
    tank = new FluidTankBase(this, CAPACITY, p -> true);
  }

  @Override
  public void tick() {
    this.syncEnergy();
    this.findMatchingRecipe();
    if (currentRecipe == null) {
      return;
    }
    this.timer--;
    if (timer < 0) {
      timer = 0;
    }
    final int cost = POWERCONF.get();
    if (energy.getEnergyStored() < cost && cost > 0) {
      return;//broke
    }
    if (timer == 0 && this.tryProcessRecipe()) {
      this.timer = TIMER_FULL;
      energy.extractEnergy(cost, false);
    }
  }

  @Override
  public void setField(int field, int value) {
    switch (Fields.values()[field]) {
      case TIMER:
        this.timer = value;
      break;
      case REDSTONE:
        this.needsRedstone = value % 2;
      break;
      case RENDER:
        this.render = value % 2;
      break;
    }
  }

  @Override
  public int getField(int field) {
    switch (Fields.values()[field]) {
      case TIMER:
        return timer;
      case REDSTONE:
        return this.needsRedstone;
      case RENDER:
        return this.render;
    }
    return 0;
  }

  @Override
  public ITextComponent getDisplayName() {
    return new StringTextComponent(getType().getRegistryName().getPath());
  }

  @Nullable
  @Override
  public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
    return new ContainerSolidifier(i, world, pos, playerInventory, playerEntity);
  }

  @Override
  public void read(BlockState bs, CompoundNBT tag) {
    tank.readFromNBT(tag.getCompound("fluid"));
    energy.deserializeNBT(tag.getCompound(NBTENERGY));
    inputSlots.deserializeNBT(tag.getCompound(NBTINV));
    outputSlot.deserializeNBT(tag.getCompound("invoutput"));
    //    outputSlotWrapper.ifPresent(h -> ((INBTSerializable<CompoundNBT>) h).deserializeNBT(tag.getCompound("invoutput")));
    super.read(bs, tag);
  }

  @Override
  public CompoundNBT write(CompoundNBT tag) {
    CompoundNBT fluid = new CompoundNBT();
    tank.writeToNBT(fluid);
    tag.put("fluid", fluid);
    tag.put(NBTENERGY, energy.serializeNBT());
    tag.put(NBTINV, inputSlots.serializeNBT());
    tag.put("invoutput", outputSlot.serializeNBT());
    return super.write(tag);
  }
  //  @Override
  //  public boolean hasFastRenderer() {
  //    return true;
  //  }

  @Override
  public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, Direction side) {
    if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
      return tankWrapper.cast();
    }
    if (cap == CapabilityEnergy.ENERGY && POWERCONF.get() > 0) {
      return energyCap.cast();
    }
    if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
      if (side == Direction.DOWN)
        return outputSlotWrapper.cast();
      return inventoryCap.cast();
    }
    return super.getCapability(cap, side);
  }

  public FluidStack getFluid() {
    return tank == null ? FluidStack.EMPTY : tank.getFluid();
  }

  public float getCapacity() {
    return CAPACITY;
  }

  @Override
  public void setFluid(FluidStack fluid) {
    tank.setFluid(fluid);
  }

  private void findMatchingRecipe() {
    if (currentRecipe != null && currentRecipe.matches(this, world)) {
      return;// its valid
    }
    currentRecipe = null;
    for (RecipeSolidifier rec : RecipeSolidifier.RECIPES) {
      if (rec.matches(this, world)) {
        currentRecipe = rec;
        break;
      }
    }
  }

  private boolean tryProcessRecipe() {
    FluidStack test = tank.drain(this.currentRecipe.getRecipeFluid(), FluidAction.SIMULATE);
    if (test.getAmount() == this.currentRecipe.getRecipeFluid().getAmount()) {
      //wait is output slot compatible
      if (!outputSlot.insertItem(0, currentRecipe.getRecipeOutput(), true).isEmpty()) {
        return false;//there was non-empty left after this, so no room for all
      }
      //ok it has room for all the fluid none will be wasted 
      inputSlots.getStackInSlot(0).shrink(1);
      inputSlots.getStackInSlot(1).shrink(1);
      inputSlots.getStackInSlot(2).shrink(1);
      tank.drain(this.currentRecipe.getRecipeFluid(), FluidAction.EXECUTE);
      outputSlot.insertItem(0, currentRecipe.getRecipeOutput(), false);
      return true;
    }
    return false;
  }

  public ItemStack getStackInputSlot(int slot) {
    return inputSlots.getStackInSlot(slot);
  }
}
