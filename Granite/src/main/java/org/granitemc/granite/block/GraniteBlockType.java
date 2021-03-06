/*
 * License (MIT)
 *
 * Copyright (c) 2014. Granite Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.granitemc.granite.block;

import org.granitemc.granite.api.block.BlockType;
import org.granitemc.granite.api.block.BlockTypes;
import org.granitemc.granite.api.item.IItemStack;
import org.granitemc.granite.item.GraniteItemStack;
import org.granitemc.granite.reflect.composite.Composite;
import org.granitemc.granite.utils.Mappings;
import org.granitemc.granite.utils.MinecraftUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GraniteBlockType extends Composite implements BlockType {

    public GraniteBlockType(Object parent) {
        super(parent);

        minecraftMetadataValuePossibilities = new HashMap<>();

        Method setValue = Mappings.getMethod("n.m.block.BlockState$StateImplementation", "setValue(n.m.block.properties.IProperty;Comparable)");
        Field values = Mappings.getField("n.m.block.BlockState$StateImplementation", "values");

        try {
            Map valuesMap = (Map) values.get(parent);
            for (Object v : valuesMap.keySet()) {
                String name = (String) Mappings.invoke(v, "n.m.block.properties.IProperty", "getName()");
                minecraftMetadataValuePossibilities.put(name, v);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> minecraftMetadataValuePossibilities;

    GraniteBlockType(Object parent, Map<String, Comparable> metadataValues) {
        this(parent);

        for (Map.Entry<String, Comparable> entry : metadataValues.entrySet()) {
            parent = setValue(parent, entry.getKey(), entry.getValue());
        }
    }

    private Object setValue(Object blockWithMetadata, String key, Comparable value) {
        try {
            Method setValue = Mappings.getMethod("n.m.block.BlockState$StateImplementation", "setValue(n.m.block.properties.IProperty;Comparable)");
            setValue.setAccessible(true);

            Object valueType = minecraftMetadataValuePossibilities.get(key);

            Object actualValue = value;

            if (valueType.getClass().isAssignableFrom(Mappings.getClass("n.m.block.BlockMetadataEnumValue"))) {
                Map map = (Map) Mappings.getField("n.m.block.BlockMetadataEnumValue", "valuesMap").get(valueType);
                actualValue = map.get(value);
            }

            return setValue.invoke(blockWithMetadata, valueType, actualValue);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public float getSlipperiness() {
        return (float) fieldGet("slipperiness");
    }

    @Override
    public int getLightOpacity() {
        return (int) fieldGet("lightOpacity");
    }

    @Override
    public int getLightValue() {
        return (int) fieldGet("lightValue");
    }

    @Override
    public float getHardness() {
        return (float) fieldGet("blockHardness");
    }

    @Override
    public float getBlastResistance() {
        return (float) fieldGet("blockResistance");
    }

    @Override
    public boolean isOpaque() {
        return getLightOpacity() == 0;
    }

    @Override
    public boolean isTransparent() {
        return !isOpaque();
    }

    @Override
    public boolean canBlockGrass() {
        return (boolean) fieldGet("canBlockGrass");
    }

    @Override
    public String getTechnicalName() {
        // Hacky hack
        return parent.toString().split(":")[1].split("\\[")[0].split(",")[0];
    }

    public IItemStack getItemStack(int amount) {
        // Super messy, it works, don't touch (pls)

        if (typeEquals(BlockTypes.air)) return null;

        Object itemStackObject = Mappings.invoke(invoke("getBlock"), "n.m.block.Block", "createStackedBlock(n.m.block.IBlockWithMetadata)", parent);

        if (Mappings.invoke(itemStackObject, "n.m.item.ItemStack", "getItem") == null) {
            Object itemObject = Mappings.invoke(null, "n.m.item.Item", "getItemFromBlock(n.m.block.Block)", invoke("getBlock"));

            if (itemObject == null) {
                itemObject = Mappings.invoke(null, "n.m.item.Item", "getItemFromName(String)", "minecraft:" + getTechnicalName());
            }

            if (itemObject == null) {
                itemObject = Mappings.invoke(getBlockObject(), "n.m.block.Block", "getItemDropped(n.m.block.IBlockWithMetadata;java.util.Random;int)", parent, new Random(), 1);
            }

            if (itemObject == null) {
                try {
                    itemObject = Mappings.getClass("n.m.item.ItemBlock")
                            .getConstructor(Mappings.getClass("n.m.block.Block"))
                            .newInstance(getBlockObject());
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

            return new GraniteItemStack(itemObject, 1);
        } else {
            GraniteItemStack graniteItemStack = (GraniteItemStack) MinecraftUtils.wrap(itemStackObject);
            graniteItemStack.setStackSize(amount);
            return graniteItemStack;
        }
    }

    @Override
    public String getName() {
        return getItemStack(1).getDisplayName();
    }

    public String getUnlocalizedName() {
        return (String) fieldGet(invoke("getBlock"), Mappings.getClass("n.m.block.Block"), "unlocalizedName");
    }

    @Override
    public int getNumericId() {
        return BlockTypes.getIdFromBlock(this);
    }

    @Override
    public Comparable getMetadata(String key) {
        return (Comparable) invoke("getValue(n.m.block.properties.IProperty)", minecraftMetadataValuePossibilities.get(key));
    }

    @Override
    public BlockType setMetadata(String key, Comparable value) {
        return (BlockType) MinecraftUtils.wrap(setValue(parent, key, value));
    }

    @Override
    public boolean equals(BlockType that) {
        GraniteBlockType thatGbt = ((GraniteBlockType) that);
        if (!thatGbt.getUnlocalizedName().equals(thatGbt.getUnlocalizedName())) return false;
        for (String key : thatGbt.minecraftMetadataValuePossibilities.keySet()) {
            if (!getMetadata(key).equals(thatGbt.getMetadata(key))) return false;
        }
        return true;
    }

    @Override
    public boolean typeEquals(BlockType that) {
        GraniteBlockType thatGbt = ((GraniteBlockType) that);
        return getUnlocalizedName().equals(thatGbt.getUnlocalizedName());
    }

    public Object getBlockObject() {
        return invoke("n.m.block.BlockState$StateImplementation", "getBlock");
    }
}
