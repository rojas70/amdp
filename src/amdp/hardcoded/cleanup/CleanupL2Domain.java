package amdp.hardcoded.cleanup;

import burlap.domain.singleagent.cleanup.CleanupWorld;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.core.objects.MutableObjectInstance;
import burlap.oomdp.core.objects.ObjectInstance;
import burlap.oomdp.core.states.MutableState;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.FullActionModel;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.ObjectParameterizedAction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author James MacGlashan.
 */
public class CleanupL2Domain implements DomainGenerator {


	@Override
	public Domain generateDomain() {

		Domain domain = new SADomain();

		Attribute inRegion = new Attribute(domain, CleanupL1Domain.ATT_IN_REGION, Attribute.AttributeType.RELATIONAL);

		Attribute colAtt = new Attribute(domain, CleanupWorld.ATT_COLOR, Attribute.AttributeType.DISC);
		colAtt.setDiscValues(CleanupWorld.COLORS);

		Attribute shapeAtt = new Attribute(domain, CleanupWorld.ATT_SHAPE, Attribute.AttributeType.DISC);
		shapeAtt.setDiscValues(CleanupWorld.SHAPES);

		ObjectClass agent = new ObjectClass(domain, CleanupWorld.CLASS_AGENT);
		agent.addAttribute(inRegion);

		ObjectClass room = new ObjectClass(domain, CleanupWorld.CLASS_ROOM);
		room.addAttribute(colAtt);

		ObjectClass block = new ObjectClass(domain, CleanupWorld.CLASS_BLOCK);
		block.addAttribute(inRegion);
		block.addAttribute(colAtt);
		block.addAttribute(shapeAtt);

		new AgentToRoomAction(CleanupL1Domain.ACTION_AGENT_TO_ROOM, domain);
		new BlockToRoomAction(CleanupL1Domain.ACTION_BLOCK_TO_ROOM, domain);

		return domain;
	}

	public static State projectToAMDPState(State s, Domain aDomain){

		State as = new MutableState();

		ObjectInstance aagent = new MutableObjectInstance(aDomain.getObjectClass(CleanupWorld.CLASS_AGENT), CleanupWorld.CLASS_AGENT);
		as.addObject(aagent);

		List<ObjectInstance> rooms = s.getObjectsOfClass(CleanupWorld.CLASS_ROOM);
		Set<String> roomNames = new HashSet<String>(rooms.size());
		for(ObjectInstance r : rooms){
			ObjectInstance ar = new MutableObjectInstance(aDomain.getObjectClass(CleanupWorld.CLASS_ROOM), r.getName());
			ar.setValue(CleanupWorld.ATT_COLOR, r.getIntValForAttribute(CleanupWorld.ATT_COLOR));
			as.addObject(ar);

			roomNames.add(r.getName());
		}

		List<ObjectInstance> blocks = s.getObjectsOfClass(CleanupWorld.CLASS_BLOCK);
		for(ObjectInstance b : blocks){
			ObjectInstance ab = new MutableObjectInstance(aDomain.getObjectClass(CleanupWorld.CLASS_BLOCK), b.getName());
			ab.setValue(CleanupWorld.ATT_COLOR, b.getIntValForAttribute(CleanupWorld.ATT_COLOR));
			ab.setValue(CleanupWorld.ATT_SHAPE, b.getIntValForAttribute(CleanupWorld.ATT_SHAPE));
			String sourceRegion = b.getStringValForAttribute(CleanupL1Domain.ATT_IN_REGION);
			if(roomNames.contains(sourceRegion)){
				ab.addRelationalTarget(CleanupL1Domain.ATT_IN_REGION, sourceRegion);
			}
			as.addObject(ab);
		}

		ObjectInstance agent = s.getFirstObjectOfClass(CleanupWorld.CLASS_AGENT);
		if(roomNames.contains(agent.getStringValForAttribute(CleanupL1Domain.ATT_IN_REGION))){
			aagent.addRelationalTarget(CleanupL1Domain.ATT_IN_REGION, agent.getStringValForAttribute(CleanupL1Domain.ATT_IN_REGION));
		}

		return as;
	}

	public static class AgentToRoomAction extends ObjectParameterizedAction implements FullActionModel{

		public AgentToRoomAction(String name, Domain domain) {
			super(name, domain, new String[]{CleanupWorld.CLASS_ROOM});
		}

		@Override
		public boolean parametersAreObjectIdentifierIndependent() {
			return false;
		}

		@Override
		public boolean applicableInState(State s, GroundedAction groundedAction) {
			ObjectParameterizedGroundedAction oga = (ObjectParameterizedGroundedAction)groundedAction;
			String curRoom = s.getFirstObjectOfClass(CleanupWorld.CLASS_AGENT).getStringValForAttribute(CleanupL1Domain.ATT_IN_REGION);
			if(!curRoom.equals(oga.params[0])){
				return true;
			}
			return false;
		}

		@Override
		public boolean isPrimitive() {
			return true;
		}

		@Override
		protected State performActionHelper(State s, GroundedAction groundedAction) {

			ObjectParameterizedGroundedAction oga = (ObjectParameterizedGroundedAction)groundedAction;
			ObjectInstance agent = s.getFirstObjectOfClass(CleanupWorld.CLASS_AGENT);
			agent.addRelationalTarget(CleanupL1Domain.ATT_IN_REGION, oga.params[0]);

			return s;
		}


		@Override
		public List<TransitionProbability> getTransitions(State s, GroundedAction groundedAction) {
			return this.deterministicTransition(s, groundedAction);
		}
	}

	public static class BlockToRoomAction extends ObjectParameterizedAction implements FullActionModel{

		public BlockToRoomAction(String name, Domain domain) {
			super(name, domain, new String[]{CleanupWorld.CLASS_BLOCK, CleanupWorld.CLASS_ROOM});
		}

		@Override
		public boolean parametersAreObjectIdentifierIndependent() {
			return false;
		}

		@Override
		public boolean applicableInState(State s, GroundedAction groundedAction) {
			ObjectParameterizedGroundedAction oga = (ObjectParameterizedGroundedAction)groundedAction;
			String curRoom = s.getObject(oga.params[0]).getStringValForAttribute(CleanupL1Domain.ATT_IN_REGION);
			if(!curRoom.equals(oga.params[1])){
				return true;
			}
			return false;
		}

		@Override
		public boolean isPrimitive() {
			return true;
		}

		@Override
		protected State performActionHelper(State s, GroundedAction groundedAction) {

			ObjectParameterizedGroundedAction oga = (ObjectParameterizedGroundedAction)groundedAction;
			ObjectInstance agent = s.getFirstObjectOfClass(CleanupWorld.CLASS_AGENT);
			ObjectInstance block = s.getObject(oga.params[0]);
			agent.clearRelationalTargets(CleanupL1Domain.ATT_IN_REGION);
			block.addRelationalTarget(CleanupL1Domain.ATT_IN_REGION, oga.params[1]);

			return s;
		}


		@Override
		public List<TransitionProbability> getTransitions(State s, GroundedAction groundedAction) {
			return this.deterministicTransition(s, groundedAction);
		}
	}


	public static void main(String[] args) {

		double lockProb = 0.25;

		CleanupWorld dgen = new CleanupWorld();
		dgen.includeDirectionAttribute(true);
		dgen.includePullAction(true);
		dgen.includeWallPF_s(true);
		dgen.includeLockableDoors(true);
		dgen.setLockProbability(lockProb);
		Domain domain = dgen.generateDomain();

		State s = CleanupWorld.getClassicState(domain);

		CleanupL1Domain a1dgen = new CleanupL1Domain();
		a1dgen.setLockableDoors(true);
		a1dgen.setLockProb(lockProb);
		Domain adomain = a1dgen.generateDomain();

		State a1s = CleanupL1Domain.projectToAMDPState(s, adomain);

		CleanupL2Domain a2dgen = new CleanupL2Domain();
		Domain a2domain = a2dgen.generateDomain();

		State a2s = CleanupL2Domain.projectToAMDPState(a1s, a2domain);

		TerminalExplorer exp = new TerminalExplorer(a2domain, a2s);
		exp.explore();

	}

}
