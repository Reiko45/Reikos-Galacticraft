/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockSpaceGlass). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import io.github.reiko45.galacticraft.oxygen.PartialSealable;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Space Glass (1.12.2 BlockSpaceGlass): double-glazed panes in a metal frame, Vanilla, Clear or Strong, with a plain or a
 * tin decoration frame. Panes join their neighbours in a straight run, a corner, a T or a cross; wherever a pane meets
 * something that isn't glass it gets a frame (a base plate below, above or at an end), and odd corners get small
 * fillets. Which of the 18 shapes, turned which of 16 ways, is worked out from the neighbours exactly as in 1.12.2. It
 * seals air sideways only, like a window.
 *
 * <p>1.12.2 worked the shape out every time the block was drawn; here it is stored in the block state and worked out
 * again whenever a neighbour changes, so a change two blocks away (the frame of the glass beside it) reaches it through
 * that neighbour's own update.
 */
public class SpaceGlassBlock extends Block implements io.github.reiko45.galacticraft.item.ShiftDescribed, PartialSealable {
    public static final EnumProperty<GlassModel> MODEL = EnumProperty.create("modeltype", GlassModel.class);
    public static final EnumProperty<GlassRotation> ROTATION = EnumProperty.create("rot", GlassRotation.class);

    private static final VoxelShape[] BOUNDING_BOXES = {
            unit(0.25, 0.0, 0.25, 0.75, 1.0, 0.75), unit(0.375, 0.0, 0.375, 0.625, 1.0, 0.625),
            unit(0.0, 0.0, 0.25, 0.75, 1.0, 0.75), unit(0.0, 0.0, 0.375, 0.625, 1.0, 0.625),
            unit(0.25, 0.0, 0.0, 0.75, 1.0, 0.75), unit(0.375, 0.0, 0.0, 0.625, 1.0, 0.625),
            unit(0.0, 0.0, 0.0, 0.75, 1.0, 0.75), unit(0.0, 0.0, 0.0, 0.625, 1.0, 0.625),
            unit(0.25, 0.0, 0.25, 1.0, 1.0, 0.75), unit(0.375, 0.0, 0.375, 1.0, 1.0, 0.625),
            unit(0.0, 0.0, 0.25, 1.0, 1.0, 0.75), unit(0.0, 0.0, 0.375, 1.0, 1.0, 0.625),
            unit(0.25, 0.0, 0.0, 1.0, 1.0, 0.75), unit(0.375, 0.0, 0.0, 1.0, 1.0, 0.625),
            unit(0.0, 0.0, 0.0, 1.0, 1.0, 0.75), unit(0.0, 0.0, 0.0, 1.0, 1.0, 0.625),
            unit(0.25, 0.0, 0.25, 0.75, 1.0, 1.0), unit(0.375, 0.0, 0.375, 0.625, 1.0, 1.0),
            unit(0.0, 0.0, 0.25, 0.75, 1.0, 1.0), unit(0.0, 0.0, 0.375, 0.625, 1.0, 1.0),
            unit(0.25, 0.0, 0.0, 0.75, 1.0, 1.0), unit(0.375, 0.0, 0.0, 0.625, 1.0, 1.0),
            unit(0.0, 0.0, 0.0, 0.75, 1.0, 1.0), unit(0.0, 0.0, 0.0, 0.625, 1.0, 1.0),
            unit(0.25, 0.0, 0.25, 1.0, 1.0, 1.0), unit(0.375, 0.0, 0.375, 1.0, 1.0, 1.0),
            unit(0.0, 0.0, 0.25, 1.0, 1.0, 1.0), unit(0.0, 0.0, 0.375, 1.0, 1.0, 1.0),
            unit(0.25, 0.0, 0.0, 1.0, 1.0, 1.0), unit(0.375, 0.0, 0.0, 1.0, 1.0, 1.0),
            unit(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), unit(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
    };

    private final GlassType type;
    private final GlassFrame frame;

    public SpaceGlassBlock(Properties properties, GlassType type, GlassFrame frame) {
        super(properties);
        this.type = type;
        this.frame = frame;
        registerDefaultState(this.stateDefinition.any().setValue(MODEL, GlassModel.STANDARD_PANE).setValue(ROTATION, GlassRotation.N));
    }

    private static VoxelShape unit(double x0, double y0, double z0, double x1, double y1, double z1) {
        return Shapes.box(x0, y0, z0, x1, y1, z1);
    }

    public GlassType type() {
        return this.type;
    }

    public GlassFrame frame() {
        return this.frame;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODEL, ROTATION);
    }

    // ---------------------------------------------------------------- neighbours (1.12.2 helpers)

    private boolean canPaneConnectTo(BlockState other) {
        return other.getBlock() == this;
    }

    private boolean buildSolidSide(BlockState other) {
        return other.getBlock() != this;
    }

    private static boolean sturdy(BlockGetter level, BlockPos pos, Direction face) {
        return level.getBlockState(pos).isFaceSturdy(level, pos, face);
    }

    private boolean isConnectedEW(BlockGetter level, BlockPos pos) {
        return canPaneConnectTo(level.getBlockState(pos.west())) || canPaneConnectTo(level.getBlockState(pos.east()));
    }

    private boolean isConnectedNS(BlockGetter level, BlockPos pos) {
        return canPaneConnectTo(level.getBlockState(pos.north())) || canPaneConnectTo(level.getBlockState(pos.south()));
    }

    private boolean isConnectedEWRecursive(BlockGetter level, BlockPos pos, Direction dir, int depth) {
        final boolean n = canPaneConnectTo(level.getBlockState(pos.north()));
        final boolean s = canPaneConnectTo(level.getBlockState(pos.south()));
        final boolean w = canPaneConnectTo(level.getBlockState(pos.west()));
        final boolean e = canPaneConnectTo(level.getBlockState(pos.east()));
        if (n || s || w || e) {
            return (w || e) && !n && !s;
        }
        final BlockPos next = pos.relative(dir);
        if (depth < 64 && level.getBlockState(next).getBlock() == this && !level.isOutsideBuildHeight(next)) {
            return isConnectedEWRecursive(level, next, dir, depth + 1);
        }
        return dir == Direction.DOWN && isPreferenceEW(level, pos);
    }

    private boolean isConnectedNSRecursive(BlockGetter level, BlockPos pos, Direction dir, int depth) {
        final boolean n = canPaneConnectTo(level.getBlockState(pos.north()));
        final boolean s = canPaneConnectTo(level.getBlockState(pos.south()));
        final boolean w = canPaneConnectTo(level.getBlockState(pos.west()));
        final boolean e = canPaneConnectTo(level.getBlockState(pos.east()));
        if (n || s || w || e) {
            return (n || s) && !w && !e;
        }
        final BlockPos next = pos.relative(dir);
        if (depth < 64 && level.getBlockState(next).getBlock() == this && !level.isOutsideBuildHeight(next)) {
            return isConnectedNSRecursive(level, next, dir, depth + 1);
        }
        return dir == Direction.DOWN && !isPreferenceEW(level, pos);
    }

    /** More solid faces east and west than north and south: a lone pane runs east-west. */
    private boolean isPreferenceEW(BlockGetter level, BlockPos pos) {
        final int ew = (sturdy(level, pos.west(), Direction.EAST) ? 1 : 0) + (sturdy(level, pos.east(), Direction.WEST) ? 1 : 0);
        final int ns = (sturdy(level, pos.north(), Direction.SOUTH) ? 1 : 0) + (sturdy(level, pos.south(), Direction.NORTH) ? 1 : 0);
        return ew > ns;
    }

    /** 1.12.2 buildSolidSideUD: does this pane need a frame at its top (or bottom) where it meets the glass above (below)? */
    private boolean buildSolidSideUD(BlockGetter level, BlockPos pos, Direction testUD) {
        final BlockPos offPos = pos.relative(testUD);
        if (level.getBlockState(offPos).getBlock() != this) {
            return true;
        }
        final int connThis = (isConnectedEW(level, pos) ? 2 : 0) + (isConnectedNS(level, pos) ? 1 : 0);
        final int connOther = (isConnectedEW(level, offPos) ? 2 : 0) + (isConnectedNS(level, offPos) ? 1 : 0);
        if (connThis == 0 && connOther == 0) {
            return false;
        }
        final int trueThis = identifyHorizConnections(level, pos);
        final int trueOther = identifyHorizConnections(level, offPos);
        if (connThis == 0 && ((trueOther & 3) == 0 && trueThis != 3 || (trueOther & 12) == 0 && trueThis == 3)) {
            return false;
        }
        if (connOther == 0 && ((trueThis & 3) == 0 && trueOther != 3 || (trueThis & 12) == 0 && trueOther == 3)) {
            return false;
        }
        if (connThis != connOther) {
            return true;
        }
        if (trueThis < 3 || trueThis == 4 || trueThis == 8) {
            return false;
        }
        if (trueOther < 3 || trueOther == 4 || trueOther == 8) {
            return false;
        }
        return (trueThis & 7) != (trueOther & 7) || (trueThis & 11) != (trueOther & 11) || (trueThis & 13) != (trueOther & 13)
                || (trueThis & 14) != (trueOther & 14);
    }

    private int identifyHorizConnections(BlockGetter level, BlockPos pos) {
        final boolean n = canPaneConnectTo(level.getBlockState(pos.north()));
        final boolean s = canPaneConnectTo(level.getBlockState(pos.south()));
        final boolean w = canPaneConnectTo(level.getBlockState(pos.west()));
        final boolean e = canPaneConnectTo(level.getBlockState(pos.east()));
        if (!n && !s && !w && !e) {
            final boolean below = level.getBlockState(pos.below()).getBlock() == this;
            final boolean above = level.getBlockState(pos.above()).getBlock() == this;
            if (below && isConnectedEWRecursive(level, pos.below(), Direction.DOWN, 0) || above && isConnectedEWRecursive(level, pos.above(), Direction.UP, 0)) {
                return 3;
            }
            if (below && isConnectedNSRecursive(level, pos.below(), Direction.DOWN, 0) || above && isConnectedNSRecursive(level, pos.above(), Direction.UP, 0)) {
                return 12;
            }
            if (isPreferenceEW(level, pos)) {
                return 3;
            }
        }
        return (n ? 8 : 0) + (s ? 4 : 0) + (w ? 2 : 0) + (e ? 1 : 0);
    }

    // ---------------------------------------------------------------- the model (1.12.2 getActualState)

    /** The shape and turn for this pane among its neighbours. */
    public BlockState withShape(BlockGetter level, BlockPos pos) {
        final BlockState north = level.getBlockState(pos.north());
        final BlockState south = level.getBlockState(pos.south());
        final BlockState west = level.getBlockState(pos.west());
        final BlockState east = level.getBlockState(pos.east());
        final boolean connectN = canPaneConnectTo(north);
        final boolean connectS = canPaneConnectTo(south);
        final boolean connectW = canPaneConnectTo(west);
        final boolean connectE = canPaneConnectTo(east);
        final boolean plateD = buildSolidSideUD(level, pos, Direction.DOWN);
        final boolean plateU = buildSolidSideUD(level, pos, Direction.UP);
        final boolean plateN = buildSolidSide(north);
        final boolean plateS = buildSolidSide(south);
        final boolean plateW = buildSolidSide(west);
        final boolean plateE = buildSolidSide(east);
        final int connections = (connectN ? 1 : 0) + (connectS ? 1 : 0) + (connectW ? 1 : 0) + (connectE ? 1 : 0);
        GlassRotation rot = GlassRotation.N;
        int cornerPiece = 0;

        if (connections == 4) {
            return model(4, rot, plateD, false, false, plateU, 0);
        }
        if (connections == 3) {
            if (!connectE) {
                rot = GlassRotation.S;
            } else if (!connectN) {
                rot = GlassRotation.E;
            } else if (!connectS) {
                rot = GlassRotation.W;
            }
            return model(3, rot, plateD, false, false, plateU, 0);
        }
        if (connections == 0) {
            final boolean below = level.getBlockState(pos.below()).getBlock() == this;
            final boolean above = level.getBlockState(pos.above()).getBlock() == this;
            if (below && isConnectedEWRecursive(level, pos.below(), Direction.DOWN, 0) || above && isConnectedEWRecursive(level, pos.above(), Direction.UP, 0)) {
                rot = GlassRotation.E;
            }
            if (!(below && isConnectedNSRecursive(level, pos.below(), Direction.DOWN, 0)) && !(above && isConnectedNSRecursive(level, pos.above(), Direction.UP, 0))
                    && isPreferenceEW(level, pos)) {
                rot = GlassRotation.E;
            }
            return model(1, rot, plateD, plateS, plateN, plateU, 0);
        }
        // a straight run east-west (or a single pane on one side)
        if (connectW && connectE || connections == 1 && (connectW || connectE)) {
            rot = GlassRotation.E;
            if (connectW && !plateU && buildSolidSideUD(level, pos.west(), Direction.UP)) {
                cornerPiece++;
            }
            if (connectW && !plateD && buildSolidSideUD(level, pos.west(), Direction.DOWN)) {
                cornerPiece += 5;
            }
            if (connectE && !plateU && buildSolidSideUD(level, pos.east(), Direction.UP)) {
                cornerPiece++;
                if (connectW) {
                    rot = GlassRotation.W;
                }
            }
            if (connectE && !plateD && buildSolidSideUD(level, pos.east(), Direction.DOWN)) {
                cornerPiece += 5;
                if (connectW) {
                    rot = GlassRotation.W;
                }
            }
            return model(1, rot, plateD, plateW, plateE, plateU, cornerPiece);
        }
        // a straight run north-south
        if (connectN && connectS || connections == 1) {
            if (connectN && !plateU && buildSolidSideUD(level, pos.north(), Direction.UP)) {
                if (!connectS) {
                    cornerPiece |= 5;
                    if (!plateD) {
                        rot = GlassRotation.S;
                    }
                } else {
                    cornerPiece |= 1;
                    rot = GlassRotation.S;
                }
            }
            if (connectN && !plateD && buildSolidSideUD(level, pos.north(), Direction.DOWN)) {
                cornerPiece++;
                if (connectS) {
                    rot = GlassRotation.S;
                }
            }
            if (connectS && !plateU && buildSolidSideUD(level, pos.south(), Direction.UP)) {
                if (cornerPiece == 0 && !plateD) {
                    cornerPiece = 5;
                    rot = GlassRotation.S;
                } else if ((cornerPiece & 3) < 2) {
                    cornerPiece++;
                }
            }
            if (connectS && !plateD && buildSolidSideUD(level, pos.south(), Direction.DOWN)) {
                if ((cornerPiece & 3) < 2) {
                    cornerPiece++;
                } else {
                    cornerPiece |= 5;
                }
            }
            return model(1, rot, plateD, plateS, plateN, plateU, cornerPiece);
        }
        // a corner
        if (connectW && connectS) {
            rot = GlassRotation.S;
        } else if (connectS && connectE) {
            rot = GlassRotation.E;
        } else if (connectN && connectW) {
            rot = GlassRotation.W;
        }
        return model(2, rot, plateD, false, false, plateU, 0);
    }

    private BlockState model(int model, GlassRotation rot, boolean plateD, boolean plateL, boolean plateR, boolean plateU, int cornerPiece) {
        int x = 0;
        int y = rot.ordinal();
        if (y % 2 == 1) {
            y ^= 2;
        }
        final int solids = (plateD ? 1 : 0) + (plateU ? 1 : 0) + (plateR ? 1 : 0) + (plateL ? 1 : 0);
        switch (model) {
            case 1:
                if (solids == 4) {
                    return shape(GlassModel.STANDARD_S4, y, 0);
                }
                if (solids == 3) {
                    if (!plateD) {
                        x = y % 2 == 1 ? 3 : 1;
                    } else if (!plateR) {
                        x = 2;
                        y ^= 2;
                    } else if (!plateU) {
                        x = y % 2 == 1 ? 1 : 3;
                    }
                    return shape(GlassModel.STANDARD_S3, y, x);
                }
                if (plateD && plateU) {
                    return shape(GlassModel.STANDARD_S2A, y, 0);
                }
                if (plateL && plateR) {
                    return shape(GlassModel.STANDARD_S2A, y, 1);
                }
                if (plateD && (plateR || plateL)) {
                    return shape(cornerPiece > 0 ? GlassModel.STANDARD_S2B : GlassModel.STANDARD_S2, plateL ? y ^ 2 : y, 0);
                }
                if (plateU && (plateR || plateL)) {
                    return shape(cornerPiece > 0 ? GlassModel.STANDARD_S2B : GlassModel.STANDARD_S2, plateL ? y ^ 2 : y, 2);
                }
                if (plateU || plateD) {
                    return shape(cornerModel(cornerPiece), y, plateU || cornerPiece > 4 ? 2 : 0);
                }
                if (plateR || plateL) {
                    return shape(cornerModel(cornerPiece), plateL ? y ^ 2 : y, 1 + (cornerPiece > 4 ? 2 : 0));
                }
                return shape(GlassModel.STANDARD_PANE, y, 0);
            case 2:
                if (plateU && plateD) {
                    return shape(GlassModel.CORNER_S2, y, 0);
                }
                if (plateD) {
                    return shape(GlassModel.CORNER_S, y, 0);
                }
                if (plateU) {
                    return shape(GlassModel.CORNER_S, (y + 3) % 4, 2);
                }
                return shape(GlassModel.CORNER, y, 0);
            case 3:
                if (plateD && plateU) {
                    return shape(GlassModel.T_JUNCTION_S2, y, 0);
                }
                if (plateD) {
                    return shape(GlassModel.T_JUNCTION_S, y, 0);
                }
                if (plateU) {
                    return shape(GlassModel.T_JUNCTION_S, y ^ 2, 2);
                }
                return shape(GlassModel.T_JUNCTION, y, x);
            case 4:
                if (plateD && plateU) {
                    return shape(GlassModel.CROSSROADS_S2, y, 0);
                }
                if (plateD) {
                    return shape(GlassModel.CROSSROADS_S, y, 0);
                }
                if (plateU) {
                    return shape(GlassModel.CROSSROADS_S, y ^ 2, 2);
                }
                return shape(GlassModel.CROSSROADS, y, x);
            default:
                return shape(GlassModel.CROSSROADS, y, x);
        }
    }

    private BlockState shape(GlassModel model, int y, int x) {
        return defaultBlockState().setValue(MODEL, model).setValue(ROTATION, GlassRotation.values()[y + x * 4]);
    }

    private static GlassModel cornerModel(int cornerPiece) {
        if ((cornerPiece & 2) == 2) {
            return GlassModel.STANDARD_S1BB;
        }
        return cornerPiece > 0 ? GlassModel.STANDARD_S1B : GlassModel.STANDARD_S1;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withShape(context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        return withShape(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // placed by code (structures, tests) without a placement context: settle into shape
        final BlockState shaped = withShape(level, pos);
        if (shaped != state && !oldState.is(this)) {
            level.setBlock(pos, shaped, Block.UPDATE_ALL);
        }
    }

    // ---------------------------------------------------------------- shapes

    /** 1.12.2 getBoundingBox: as wide as the frame where there is one, the glass elsewhere, out to the joined sides. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        final BlockState above = level.getBlockState(pos.above());
        final BlockState below = level.getBlockState(pos.below());
        final BlockState north = level.getBlockState(pos.north());
        final BlockState south = level.getBlockState(pos.south());
        final BlockState west = level.getBlockState(pos.west());
        final BlockState east = level.getBlockState(pos.east());
        final boolean connectedN = canPaneConnectTo(north);
        final boolean connectedS = canPaneConnectTo(south);
        final boolean connectedW = canPaneConnectTo(west);
        final boolean connectedE = canPaneConnectTo(east);
        final boolean plateD = buildSolidSide(below);
        final boolean plateU = buildSolidSide(above);
        boolean plateN = buildSolidSide(north);
        boolean plateS = buildSolidSide(south);
        boolean plateW = buildSolidSide(west);
        boolean plateE = buildSolidSide(east);
        if (connectedN || connectedS) {
            plateW = plateE = false;
        }
        if (connectedW || connectedE) {
            plateN = plateS = false;
        }
        if (!connectedE && !connectedW && !connectedN && !connectedS) {
            if (preferEWForSingle(level, pos, above, below)) {
                plateN = plateS = false;
            } else {
                plateW = plateE = false;
            }
        }
        int offset = 0;
        int base = 1;
        final int solids = (plateD ? 1 : 0) + (plateU ? 1 : 0) + (plateN ? 1 : 0) + (plateS ? 1 : 0) + (plateW ? 1 : 0) + (plateE ? 1 : 0);
        if (solids > 2 || plateU && plateD || plateW && plateE || plateN && plateS) {
            base = 0;
        }
        if (connectedW || plateW) {
            offset = 2;
        }
        if (connectedN || plateN) {
            offset += 4;
        }
        if (connectedE || plateE) {
            offset += 8;
        }
        if (connectedS || plateS) {
            offset += 16;
        }
        return BOUNDING_BOXES[offset + base];
    }

    private boolean preferEWForSingle(BlockGetter level, BlockPos pos, BlockState above, BlockState below) {
        boolean prefEW = false;
        if (below.getBlock() == this) {
            prefEW = isConnectedEW(level, pos.below()) || isPreferenceEW(level, pos.below());
        } else if (above.getBlock() == this) {
            prefEW = isConnectedEW(level, pos.above()) || isPreferenceEW(level, pos.above());
        }
        return prefEW || isPreferenceEW(level, pos);
    }

    /** 1.12.2 addCollisionBoxToList: the frame's width if framed, else the glass; diagonals at corners. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        final BlockState above = level.getBlockState(pos.above());
        final BlockState below = level.getBlockState(pos.below());
        final BlockState north = level.getBlockState(pos.north());
        final BlockState south = level.getBlockState(pos.south());
        final BlockState west = level.getBlockState(pos.west());
        final BlockState east = level.getBlockState(pos.east());
        final boolean connectN = canPaneConnectTo(north);
        final boolean connectS = canPaneConnectTo(south);
        final boolean connectW = canPaneConnectTo(west);
        final boolean connectE = canPaneConnectTo(east);
        final boolean plateD = buildSolidSide(below);
        final boolean plateU = buildSolidSide(above);
        boolean plateN = buildSolidSide(north);
        boolean plateS = buildSolidSide(south);
        boolean plateW = buildSolidSide(west);
        boolean plateE = buildSolidSide(east);
        if (connectN || connectS) {
            plateW = plateE = false;
        }
        if (connectW || connectE) {
            plateN = plateS = false;
        }
        if (!connectE && !connectW && !connectN && !connectS) {
            if (preferEWForSingle(level, pos, above, below)) {
                plateN = plateS = false;
            } else {
                plateW = plateE = false;
            }
        }
        final double posGlass = 0.375D;
        final double posBase = 0.225D;
        double xMin = plateU || plateD || plateN || plateS ? posBase : posGlass;
        double xMax = plateU || plateD || plateN || plateS ? 1.0D - posBase : 1.0D - posGlass;
        double zMin = plateU || plateD || plateE || plateW ? posBase : posGlass;
        double zMax = plateU || plateD || plateE || plateW ? 1.0D - posBase : 1.0D - posGlass;
        if (plateW || connectW) {
            xMin = 0.0D;
        }
        if (plateE || connectE) {
            xMax = 1.0D;
        }
        if (plateN || connectN) {
            zMin = 0.0D;
        }
        if (plateS || connectS) {
            zMax = 1.0D;
        }
        if ((connectW ^ connectE) && (connectN ^ connectS) && !(plateU && plateD)) {
            // 1.12.2 shrank a lone corner to a thin diagonal post
            final double diag = 0.25D;
            if (connectW) {
                xMin = diag - 0.01D;
                xMax = diag;
            } else {
                xMin = 1.0D - diag;
                xMax = 1.01D - diag;
            }
            if (connectN) {
                zMin = diag - 0.01D;
                zMax = diag;
            } else {
                zMin = 1.0D - diag;
                zMax = 1.01D - diag;
            }
        }
        return Shapes.box(xMin, 0.0D, zMin, Math.min(xMax, 1.0D), 1.0D, Math.min(zMax, 1.0D));
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return false;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** 1.12.2 isSealed: sideways only, like a window. */
    @Override
    public boolean isSealed(Level level, BlockPos pos, BlockState state, Direction direction) {
        return direction.getAxis().isHorizontal();
    }

    /** 1.12.2 GlassModel: the 18 pane shapes. */
    public enum GlassModel implements StringRepresentable {
        STANDARD_PANE("standard", "ns", null), CORNER("corner", "ne", null), T_JUNCTION("joint", "nse", null), CROSSROADS("joinx", "nsew", null),
        STANDARD_S1("standards1", "ns", "s1"), STANDARD_S1B("standards1b", "ns", "s1b"), STANDARD_S1BB("standards1bb", "ns", "s1bb"),
        STANDARD_S2("standards2", "ns", "s2"), STANDARD_S2A("standards2a", "ns", "s2a"), STANDARD_S2B("standards2b", "ns", "s2b"),
        STANDARD_S3("standards3", "ns", "s3"), STANDARD_S4("standards4", "ns", "s4"), CORNER_S("corner_s", "ne", "s1"),
        CORNER_S2("corner_s2", "ne", "s2"), T_JUNCTION_S("joint_s", "nse", "s1"), T_JUNCTION_S2("joint_s2", "nse", "s2"),
        CROSSROADS_S("joinx_s", "nsew", "s1"), CROSSROADS_S2("joinx_s2", "nsew", "s2");

        private final String name;
        private final String arms;
        private final String frame;

        GlassModel(String name, String arms, String frame) {
            this.name = name;
            this.arms = arms;
            this.frame = frame;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        /** The 1.12.2 pane model (models/block/glass/glass_...). */
        public String glassModel() {
            return "glass_" + this.arms + (this.frame == null ? "" : "_" + this.frame);
        }

        /** The 1.12.2 frame model (models/block/glass/base_...), or null for unframed shapes. */
        public String frameModel() {
            return this.frame == null ? null : "base_" + this.arms + "_" + this.frame;
        }
    }

    /**
     * 1.12.2 GlassRotation, with the vanilla x/y block-state rotation that matches each of its Forge transforms (an x tilt
     * of 0, 90, 180 or 270, then a turn about y).
     */
    public enum GlassRotation implements StringRepresentable {
        N("0", 0, 0), E("y", 0, 270), S("yy", 0, 180), W("yyy", 0, 90),
        UN("u", 270, 0), UE("uyyy", 90, 90), US("uyy", 270, 180), UW("uy", 90, 270),
        UUN("uuyy", 180, 180), UUE("uuyyy", 180, 90), UUS("uu", 180, 0), UUW("uuy", 180, 270),
        DN("d", 90, 0), DE("dy", 270, 270), DS("dyy", 90, 180), DW("dyyy", 270, 90);

        private final String name;
        private final int x;
        private final int y;

        GlassRotation(String name, int x, int y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public int x() {
            return this.x;
        }

        public int y() {
            return this.y;
        }
    }

    /** 1.12.2 GlassType. */
    public enum GlassType {
        VANILLA, CLEAR, STRONG
    }

    /** 1.12.2 GlassFrame: the frame texture and its tint (1.12.2 defaultColor). */
    public enum GlassFrame {
        PLAIN(0xFAFAF7), TIN_DECO(0xFFFFFF);

        private final int color;

        GlassFrame(int color) {
            this.color = color;
        }

        public int color() {
            return this.color;
        }
    }

    /** 1.12.2 getShiftDescription: a text shared with others like it. */
    @Override
    public String shiftDescriptionKey(net.minecraft.world.item.ItemStack stack) {
        return switch (this.type) {
            case CLEAR -> "block.galacticraftcore.space_glass.clear.description";
            case VANILLA -> "block.galacticraftcore.space_glass.description";
            case STRONG -> "block.galacticraftcore.space_glass.strong.description";
        };
    }
}
