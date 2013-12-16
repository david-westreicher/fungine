var sys;
(function ($) {

    var Renderer = function (canvas) {
        var canvas = $(canvas).get(0)
        var ctx = canvas.getContext("2d");
        var particleSystem

        var that = {
            fullGraph: false,
            init: function (system) {
                particleSystem = system
                particleSystem.screenSize(canvas.width, canvas.height)
                particleSystem.screenPadding(20)
                that.initMouseHandling()
            },

            redraw: function () {
                ctx.fillStyle = "white"
                ctx.fillRect(0, 0, canvas.width, canvas.height)

                particleSystem.eachEdge(function (edge, pt1, pt2) {
                    var rot = Math.atan2(pt2.y - pt1.y, pt2.x - pt1.x);
                    var start = {
                        x: pt1.x + Math.cos(rot) * 20,
                        y: pt1.y + Math.sin(rot) * 20
                    };
                    var end = {
                        x: pt2.x - Math.cos(rot) * 20,
                        y: pt2.y - Math.sin(rot) * 20
                    };
                    var cp = {
                        x: Math.cos(rot + Math.PI / 2) * 10,
                        y: Math.sin(rot + Math.PI / 2) * 10
                    };
                    ctx.save()
                    ctx.strokeStyle = edge.data.col || "rgba(0,0,0, .333)";
                    ctx.lineWidth = 1
                    ctx.beginPath()
                    ctx.moveTo(start.x, start.y)
                    ctx.bezierCurveTo(start.x + cp.x, start.y + cp.y, end.x + cp.x, end.y + cp.y, end.x, end.y)
                    ctx.stroke()
                    ctx.restore()
                    ctx.save()
                    ctx.fillStyle = edge.data.col || "rgba(0,0,0, .333)"
                    ctx.beginPath();
                    ctx.arc(end.x, end.y, 5, rot - 2, rot + 2);
                    ctx.fill();
                    ctx.font = "12px Helvetica"
                    ctx.textAlign = "center"
                    ctx.fillText(edge.data.label || "asd", (start.x + end.x) / 2 + cp.x * 2, (start.y + end.y) / 2 + cp.y * 2)
                    ctx.restore()
                })

                particleSystem.eachNode(function (node, pt) {
                    // node: {mass:#, p:{x,y}, name:"", data:{}}
                    // pt:   {x:#, y:#}  node position in screen coords

                    // draw a rectangle centered at pt
                    //var w = 10
                    ctx.font = "12px Helvetica"
                    ctx.textAlign = "center"
                    ctx.fillStyle = "black"
                    ctx.fillText(node.data.label || "not defined", pt.x, pt.y + 4)
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
                        dragged.node.tempMass = 1000
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
        sys = arbor.ParticleSystem(1000, 1000, 0.5) // create the system with sensible repulsion/stiffness/friction
        sys.parameters({
            gravity: true
        }) // use center-gravity to make the graph settle nicely (ymmv)
        sys.renderer = Renderer("#viewport") // our newly created renderer will have its .init() method called shortly by sys...

		sys.addNode("david", {
            label: "david"
        });
		sys.addNode("nat", {
            label: "nat"
        });
		sys.addEdge("david", "nat", {
            label: "edge",
            col: "rgba(255,0,0, 0.5)"
        });

    })

})(this.jQuery)