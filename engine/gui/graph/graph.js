var sys;
(function ($) {

    var Renderer = function (canvas) {
        var canvas = $(canvas).get(0)
        var ctx = canvas.getContext("2d");
        var particleSystem;
        var that = {
            fullGraph: false,
            init: function (system) {
                particleSystem = system
                particleSystem.screenSize(canvas.width, canvas.height)
                particleSystem.screenPadding(0)
                that.initMouseHandling()
            },

            redraw: function () {
				ctx.clearRect (0, 0, canvas.width, canvas.height);
                //ctx.fillStyle = "rgba(255,255,255,0.4)";
                //ctx.fillRect(0, 0, canvas.width, canvas.height);
                particleSystem.eachEdge(function (edge, pt1, pt2) {
                    var rot = Math.atan2(pt2.y - pt1.y, pt2.x - pt1.x);
                    
                    ctx.save()
                    ctx.strokeStyle = edge.data.col || "rgba(0,0,0, .333)";
					var outs = edge.source.data.outs;
					var ins = edge.target.data.ins;
					var outHeight = 28;
					for(var i=0;i<outs.length;i++){
						if(edge.data.out==outs[i])
							break;
						outHeight+=15;
					}
					var inHeight = 28;
					for(var i=0;i<ins.length;i++){
						if(edge.data.inp==ins[i])
							break;
						inHeight+=15;
					}
					var width = 150;
                    ctx.lineWidth = 2
                    ctx.beginPath()
                    ctx.moveTo(pt1.x+width/2+5, pt1.y+outHeight)
                    ctx.lineTo(pt2.x-width/2-5, pt2.y+inHeight);
                    ctx.stroke()
                    ctx.restore()
                })

                particleSystem.eachNode(function (node, pt) {
                    // node: {mass:#, p:{x,y}, name:"", data:{}}
                    // pt:   {x:#, y:#}  node position in screen coords

                    // draw a rectangle centered at pt
                    //var w = 10
                    ctx.font = "bold 13px Helvetica"
                    ctx.textAlign = "center"
                    ctx.fillStyle = "white"
                    ctx.fillText(node.data.label || "not defined", pt.x, pt.y + 8)
                    ctx.font = "12px Helvetica"
                    ctx.strokeStyle = "rgb(255,255,255)";
                    ctx.lineWidth = 2
					var paramSize = Math.max(node.data.ins.length,node.data.outs.length);
					var width = 150;
					var height = (paramSize+1)*15;
					var midX = pt.x-width/2-5;
					var midY = pt.y-height/2-5+height/2;
					ctx.beginPath();
					ctx.moveTo(10+midX, midY);
					ctx.lineTo(width+midX, midY);
					ctx.quadraticCurveTo(width+midX+10, midY, width+midX+10, 10+midY);
					ctx.lineTo(width+midX+10, height+midY);
					ctx.quadraticCurveTo(width+midX+10, height+midY+10, width+midX, height+midY+10);
					ctx.lineTo(10+midX, height+midY+10);
					ctx.quadraticCurveTo(midX, height+midY+10, midX, height+midY);
					ctx.lineTo(midX, 10+midY);
					ctx.quadraticCurveTo(midX, midY, 10+midX, midY);
					ctx.stroke();	
                    ctx.textAlign = "left"
					for(var i=0;i<node.data.ins.length;i++){
						var posX = pt.x-width/2;
						var posY = pt.y + (i+2)*15;
	                    ctx.fillText(node.data.ins[i], posX, posY)
						ctx.beginPath();
						ctx.arc(posX-5, posY-4, 5, 0, 2 * Math.PI, false);
						ctx.fill();
					}
                    ctx.textAlign = "right"
					for(var i=0;i<node.data.outs.length;i++){
						var posX = pt.x+width/2;
						var posY = pt.y + (i+2)*15;
	                    ctx.fillText(node.data.outs[i], posX, posY)
						ctx.beginPath();
						ctx.arc(posX+5, posY-4, 5, 0, 2 * Math.PI, false);
						ctx.fill();
					}
                })
            },

            initMouseHandling: function () {
                // no-nonsense drag and drop (thanks springy.js)
                var dragged = null;

                // set up a handler object that will initially listen for mousedowns then
                // for moves and mouseups while dragging
                var handler = {
                    clicked: function (e) {
                        var pos = $(canvas).offset();
                        _mouseP = arbor.Point(e.pageX - pos.left, e.pageY - pos.top)
                        dragged = particleSystem.nearest(_mouseP);
						window.nodeSelect(dragged.node.data);

                        if(dragged && dragged.node !== null) {
                            // while we're dragging, don't let physics move the node
                            dragged.node.fixed = true
                        }

                        $(canvas).bind('mousemove', handler.dragged)
                        $(window).bind('mouseup', handler.dropped)

                        return false
                    },
                    dblclick: function (e) {
                        var pos = $(canvas).offset();
                        _mouseP = arbor.Point(e.pageX - pos.left, e.pageY - pos.top)
                        dragged = particleSystem.nearest(_mouseP);
                        if(dragged && dragged.node !== null)
                            this.currentSelection = dragged.node;
                        return false
                    },
                    dragged: function (e) {
                        var pos = $(canvas).offset();
                        var s = arbor.Point(e.pageX - pos.left, e.pageY - pos.top)

                        if(dragged && dragged.node !== null) {
                            var p = particleSystem.fromScreen(s)
                            dragged.node.p = p
                        }

                        return false
                    },

                    dropped: function (e) {
                        if(dragged === null || dragged.node === undefined) return
                        if(dragged.node !== null) dragged.node.fixed = false
                        dragged.node.tempMass = 10
                        dragged = null
                        $(canvas).unbind('mousemove', handler.dragged)
                        $(window).unbind('mouseup', handler.dropped)
                        _mouseP = null
                        return false
                    }
                }

                // start listening
                $(canvas).mousedown(handler.clicked);
                $(canvas).dblclick(handler.dblclick);

            },

        }
        return that
    }

    $(document).ready(function () {
        sys = arbor.ParticleSystem(8, 10, 0.5) // create the system with sensible repulsion/stiffness/friction
        sys.parameters({
            gravity: true
        }) // use center-gravity to make the graph settle nicely (ymmv)
        sys.renderer = Renderer("#viewport") // our newly created renderer will have its .init() method called shortly by sys...

		var addNodes = function(nodes){
			for(var i=0;i<nodes.length;i++){
				var n = nodes[i];
				var ins = [];
				var outs = [];
				for(var j=0;j<n["in"].length;j++){
					ins.push(n["in"][j].name);
				}
				for(var j=0;j<n["out"].length;j++){
					outs.push(n["out"][j].name);
				}
				sys.addNode(n.id, {
		        	label: n.id,
					ins:ins,
					outs:outs,
					internals:n.internals
		    	});
			}
		}

		window.initGraph = function(e){
			addNodes(e.shaderNodes);
			addNodes(e.initableNodes);
			addNodes(e.renderNodes);
			addNodes(e.nodes);
			var edges = e.connections;
			for(var i=0;i<edges.length;i++){
				var edge = edges[i];
				sys.addEdge(edge.outID, edge.inID, {
				    label: "edge",
					out: edge["out"].name,
					inp: edge["in"].name,
				    col: "rgba(255,255,255, 1)"
				});
			}
		}
    })

})(this.jQuery)
