/*************************************************************************
 *                                                                       *
 * Open Dynamics Engine, Copyright (C) 2001,2002 Russell L. Smith.       *
 * All rights reserved.  Email: russ@q12.org   Web: www.q12.org          *
 * Open Dynamics Engine 4J, Copyright (C) 2007-2013 Tilmann Zaeschke     *
 * All rights reserved.  Email: ode4j@gmx.de   Web: www.ode4j.org        *
 *                                                                       *
 * This library is free software; you can redistribute it and/or         *
 * modify it under the terms of EITHER:                                  *
 *   (1) The GNU Lesser General Public License as published by the Free  *
 *       Software Foundation; either version 2.1 of the License, or (at  *
 *       your option) any later version. The text of the GNU Lesser      *
 *       General Public License is included with this library in the     *
 *       file LICENSE.TXT.                                               *
 *   (2) The BSD-style license that is included with this library in     *
 *       the file ODE-LICENSE-BSD.TXT and ODE4J-LICENSE-BSD.TXT.         *
 *                                                                       *
 * This library is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the files    *
 * LICENSE.TXT, ODE-LICENSE-BSD.TXT and ODE4J-LICENSE-BSD.TXT for more   *
 * details.                                                              *
 *                                                                       *
 *************************************************************************/
package physics;

import java.util.ArrayList;
import java.util.List;

import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DContactJoint;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DGeom.DNearCallback;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DPlane;
import org.ode4j.ode.DSimpleSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.OdeMath;

public class OdeTest {
	protected static final int MAX_CONTACTS = 100;
	private DWorld world;
	private DSimpleSpace space;
	private List<DBody> bodies = new ArrayList<DBody>();
	private DJointGroup jointGroup = OdeHelper.createJointGroup();

	public OdeTest() {

		// create world
		OdeHelper.initODE2(0);
		world = OdeHelper.createWorld();
		space = OdeHelper.createSimpleSpace();
		world.setGravity(0, 0, -0.5);
		DPlane ground = OdeHelper.createPlane(space, 0, 0, 1, 0);

		for (int i = 0; i < 100; i++) {
			DBox box = OdeHelper.createBox(space, 1, 1, 1);
			DBody body = OdeHelper.createBody(world);
			DMass mass = OdeHelper.createMass();
			mass.setBoxTotal(1, 1, 1, 1);
			body.setMass(mass);
			bodies.add(body);
			body.setPosition(Math.random() * 10, Math.random() * 10,
					Math.random() * 10 + 1);
			box.setBody(body);
		}

		// run simulation
		// dsSimulationLoop("", 352, 288, this);
		for (int k = 0; k < 100; k++) {
			space.collide(null, new DNearCallback() {

				public void call(Object arg0, DGeom o1, DGeom o2) {
					DContactBuffer contacts = new DContactBuffer(MAX_CONTACTS);
					for (int i = 0; i < MAX_CONTACTS; i++) {
						DContact contact = contacts.get(i);
						contact.surface.mode = OdeMath.dContactBounce;
						contact.surface.mu = 50000;
						contact.surface.bounce = 0.1;
					}
					int contactNum = OdeHelper.collide(o1, o2, 10,
							contacts.getGeomBuffer());
					for (int i = 0; i < contactNum; i++) {
						DContactJoint joint = OdeHelper.createContactJoint(
								world, jointGroup, contacts.get(i));
						joint.attach(o1.getBody(), o2.getBody());
					}
				}
			});
			world.step(0.1);
			jointGroup.empty();
		}
		for (DBody body : bodies)
			body.destroy();
		space.destroy();
		world.destroy();
		OdeHelper.closeODE();
	}

	public static void main(String args[]) {
		System.out.println("asd");
		new OdeTest();
	}
}