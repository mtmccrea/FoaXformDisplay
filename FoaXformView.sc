// Used by FoaXformDisplay

FoaXformView {
	// copyArgs
	var sfView, target, <initChainDex, <initDex;
	var <>view, <>layout, <>labelTxt, <>name, <>mtx;
	var <ctlLayout, <addRmvLayout, <xFormMenu;
	var <inputMenu, <chain, colorsMuted=false;
	var muteBut, soloBut;


	*new { |sfView, target = 'chain', chainDex, index|

		sfView.debug.if{"creating new XForm".postln;};
		^super.newCopyArgs(sfView, target, chainDex, index).init;
	}


	init {
		sfView.debug.if{"initializing new XForm".postln;};

		chain = switch(target,
			'chain',	{sfView.chain},						// xform in chain view UI
			'display',	{sfView.displayChain} 	// or xform view in the xformDisplay UI
		);

		// if this xform takes a chain index for an input,
		// this var is used to access its dropdown menu for resetting when
		// chain elements are added or removed
		inputMenu = nil;

		// layout to hold addRmvLayout and ctlLayout
		layout = HLayout().margins_(sfView.xfMargins);

		// view holding all elements in the xForm panel
		view = View()
		.background_(Color.hsv(
			*sfView.ctlColor.asHSV.put(0,
				(sfView.ctlColor.asHSV[0] + (initChainDex * sfView.colorStep)).fold(0,1)))
		)
		.fixedHeight_(80)
		.minWidth_(sfView.chainViewWidth)
		.layout_(layout)
		;

		// the chain ID of this transform, e.g. "A3"
		labelTxt = StaticText().string_("id")
		.stringColor_(Color.hsv(
			*sfView.idTxtColor.asHSV.put(0,
				(sfView.idTxtColor.asHSV[0] + (initChainDex * sfView.colorStep)).fold(0,1))))
		.align_(\center);

		// id, "X", "+" layout
		addRmvLayout = VLayout().margins_(sfView.addRmvMargins);

		// transform controls layout
		ctlLayout = VLayout();
		layout.add(ctlLayout);
		// this allows the ctlLayout to move all the way left
		if (target == 'chain') {layout.add(nil)};

		name = chain.chains[initChainDex][initDex].name;

		if (initDex == 0)
		{
			this.addAddRmvButs(includeRmv: false);
			ctlLayout.add(StaticText().string_(name).align_(\left));

			// the original soundfield has no controls
			if (initChainDex != 0) {
				// an input soundfield at the head of a chain
				this.addInputMenuCtl('this index', 0);
			};
		} {
			if (target == 'chain') {this.addAddRmvButs(true)};
			// add the first dropdown, no controls until a menu item selected
			// this assumes a 'soundfield thru' transform right when created
			this.addTransformMenu();
		};
	}


	addTransformMenu { |selectedName|
		var items;

		sfView.debug.if{"adding a menu".postln;};

		items = [];
		chain.xFormDict_sorted.do{ |me|
			// exclude 'a/input soundfield', which is only used by first chain transform
			if((me[0] != 'input soundfield') and: (me[0] != 'a soundfield'))
			{items = items.add(me[0])};
		};
		items = ['-'] ++ items;

		xFormMenu = PopUpMenu().items_(items)
		.action_({ |mn|
			var chDex, dex;

			// recreate the StaticText because the original will be removed
			labelTxt = StaticText().string_(labelTxt.string).align_(\center)
			.stringColor_(
				Color.hsv(
					*sfView.idTxtColor.asHSV.put(0,
						(sfView.idTxtColor.asHSV[0]
							+ (initChainDex * sfView.colorStep)).fold(0,1)))
			);

			// '-' mutes the transform
			name = if(mn.item == '-') {'mute'} {mn.item};

			#chDex, dex = this.getViewIndex;
			chain.replaceTransform(name, chDex, dex);
		})
		.maxWidth_(125).minWidth_(95).value_(0);

		ctlLayout.add(xFormMenu, align: \left);

		if(selectedName != 'mute') {
			// update with the current transform selection
			xFormMenu.value_(xFormMenu.items.indexOf(selectedName))
		};
	}


	// called when selecting a new control from the dropdown menu
	// rebuild the view with the new controls
	rebuildControls {
		// nil this var, it's reset as needed when rebuilt
		inputMenu = nil;

		sfView.debug.if{postf("rebuilding controls: %\n", name)};

		// clear the view's elements
		view.children.do(_.remove);

		if(target == 'chain', {this.addAddRmvButs});

		// add menu back, with new xform selected
		this.addTransformMenu(name);

		// don't rebuild controls if muted
		if(name != 'mute') {
			var controls;
			controls = chain.xFormDict[ name ].controls.clump(2);

			controls.do{ |pair, i|
				var ctlName, ctl;
				#ctlName, ctl = pair;
				case
				{ctl.isKindOf(ControlSpec)} {
					this.addSliderCtl(ctlName, ctl, i);
				}
				// a ctl of a Symbol, e.g. 'A0' yields a
				// dropdown menu for an input soundfield
				{ctl.isKindOf(Symbol)} {
					this.addInputMenuCtl(ctlName, i);
				}
			};
		};
	}


	addInputMenuCtl { |xfname, ctlOrder|
		var menu, items, dropLayout;
		var chDex, dex;

		#chDex, dex = if(initDex == 0,
			{[ initChainDex, initDex ]},
			{this.getViewIndex}
		);

		items = sfView.prGetInputList(chDex, dex);

		menu = PopUpMenu().items_(items).value_(0)
		.action_({ |mn|
			var chDex, dex;
			#chDex, dex = this.getViewIndex;

			chain.setParam(chDex, dex, ctlOrder, mn.item);
		});

		dropLayout = HLayout(
			StaticText().string_(xfname.asString).align_('left'),
			menu
		);
		ctlLayout.add(dropLayout);

		// update the instance variable
		inputMenu = menu;
	}

	addSliderCtl { |ctlName, spec, ctlOrder|
		var min, max, sl, nb, nameTxt, unitTxt, slLayout;

		// min/maxItem to handle negative max on rotate xform
		min = [spec.minval, spec.maxval].minItem;
		max = [spec.minval, spec.maxval].maxItem;

		sl = Slider()
		.action_({ |sldr|
			var val, chDex, dex;
			#chDex, dex = this.getViewIndex;

			val = spec.map(sldr.value);
			if(spec.units == "π",
				{nb.value_(val.round(0.001) / pi)},
				{nb.value_(val.round(0.001))}
			);
			chain.setParam(chDex, dex, ctlOrder, val);
			}
		).orientation_('horizontal')
		.value_(spec.unmap(spec.default));

		nb = NumberBox()
		.action_({ |nb|
			var val, chDex, dex;
			#chDex, dex = this.getViewIndex;

			if(spec.units == "π",
				{val = nb.value * pi;},
				{val = nb.value;}
			);
			sl.value_(spec.unmap(val));
			chain.setParam(chDex, dex, ctlOrder, val);
			}
		)
		.clipHi_(
			if(spec.units == "π"){max / pi;}{max})
		.clipLo_(
			if(spec.units == "π"){min / pi;}{min})
		.fixedWidth_(45)
		.step_(0.01).minDecimals_(3);

		unitTxt = StaticText().string_(spec.units)
		.align_('left').minWidth_(20).maxWidth_(20);

		nameTxt = StaticText().string_(ctlName.asString)
		.align_('left').minWidth_(50);

		slLayout =  HLayout();

		[nameTxt, sl, nb, unitTxt].do{ |me| slLayout.add(me)};

		ctlLayout.add(slLayout);
	}


	addAddRmvButs { |includeRmv = true|
		var ht = 20, wth = 20, addRmvView, addBut, rmvBut, lay;

		addRmvView = View().layout_(addRmvLayout)
		.fixedWidth_(50)
		.background_(Color.hsv(
			*sfView.idTabColor.asHSV.put(0,
				(sfView.idTabColor.asHSV[0] + (initChainDex * sfView.colorStep)).fold(0,1)))
		);

		view.layout.insert(addRmvView, align: \left);

		rmvBut = Button()
		.states_([[ "X" ]]).maxHeight_(ht).maxWidth_(wth)
		.action_({ |but|
			var myChain, myID;
			#myChain, myID = sfView.prGetXfViewID(this);
			// remove the transform from the matrix chain
			chain.removeTransform(myChain, myID);
		});

		addBut = Button()
		.states_([["+"]]).maxHeight_(ht).maxWidth_(wth)
		.action_({ |but|
			var myChain, myID;

			#myChain, myID = sfView.prGetXfViewID(this);
			chain.addTransform('soundfield thru', myChain, myID + 1);
		});

		muteBut = StaticText().string_("M")
		.stringColor_(Color.gray)
		.minWidth_(25).align_(\center)
		.mouseUpAction_({
			var myChain, myID, muteState;
			#myChain, myID = sfView.prGetXfViewID(this);
			muteState = chain.chains[myChain][myID].muted;
			chain.muteXform(muteState.not, myChain, myID);
		});

		soloBut = StaticText().string_("S")
		.stringColor_(Color.gray)
		.minWidth_(25).align_(\center)
		.mouseUpAction_({
			var myChain, myID, soloState;
			#myChain, myID = sfView.prGetXfViewID(this);
			soloState = chain.chains[myChain][myID].soloed;
			chain.soloXform(soloState.not, myChain, myID);
		});


		lay = if(includeRmv){
			VLayout(
				[labelTxt,	a: 'bottom'],
				HLayout(
					[rmvBut,	a: 'center'],
					[muteBut,	a: 'center'],
				),
				HLayout(
					[addBut,	a: 'center'],
					[soloBut,	a: 'center'],
				)
			)
		}{
			VLayout(
				[labelTxt,	a: 'top'],
				300, // force it to fill the height of the view
				[addBut,	a: 'bottom']
			)
		};

		addRmvLayout.add(lay);
	}

	getViewIndex {
		^switch(target,
			'chain', 	{sfView.prGetXfViewID(this)},
			'display',	{[ 0, 1 ]}
		);
	}

	// updates xf view colors according to mute/solo state
	// also upddates mute/solo button state
	muteState { |bool|
		this.updateStateColors(bool);
		if (bool) {
			muteBut.stringColor_(Color.red);
			muteBut.background_(Color.gray);
		} {
			muteBut.stringColor_(Color.gray);
			muteBut.background_(Color.clear);
		}
	}

	soloState { |bool|
		/*this.updateStateColors(bool);*/
		if (bool) {
			soloBut.stringColor_(Color.yellow);
			soloBut.background_(Color.gray);
		} {
			soloBut.stringColor_(Color.gray);
			soloBut.background_(Color.clear);
		}
	}

	// set the nested views to "muted" colors
	// used by both mute and solo functions
	updateStateColors { |darken=true|
		var satFac, valFac, colFunc;
		var chDex, dex;

		if (colorsMuted == darken) {^this}; // state matches, return
		if (darken) {
			satFac = 0.7;
			valFac = 0.7;
		} {
			satFac = 0.7.reciprocal;
			valFac = 0.7.reciprocal;
		};

		colFunc = { |v|
			var col, newCol;
			col = try {v.background}{^this}; // return if view doesn't respond to .background
			col.notNil.if{
				"trying ".post;
				newCol = Color.hsv(
					*col.asHSV.round(0.01)*[1,satFac,valFac,1] // round to avoid accumulating error over many un/mutes
				);
				v.background = newCol.postln;
			};
		};
		this.prFindKindDo(this.view, View, colFunc);
		"col updated\n".postln;
		colFunc.(this.view); // change the topmost view as well
		colorsMuted = darken;
	}

	prFindKindDo { |view, kind, performFunc|
		view.children.do{ |child|
			child.isKindOf(View).if{
				this.prFindKindDo(child, kind, performFunc)   // call self
			};
			child.isKindOf(kind).if{performFunc.(child)};
		};
	}
}
