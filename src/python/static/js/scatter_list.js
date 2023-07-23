
const width = htmlwidth
const height = htmlheight

const scatterPlot = d3
  .select("body")
  .select("#scatter")
  .append("svg")
  .attr("viewBox", "0 0 " + width + " " + height)
  .style("background-color", "white")
  .style("margin-top", "0.3%")
  



const renderScatterPlot = (data) => { 
    data.shift();
    console.log("Hallo");
    console.log(color);
    margin = { top: 70, right: 50, bottom: 80, left: 80 };
    const innerWidth = width - margin.right - margin.left;
    const innerHeight = height - margin.top - margin.bottom;

    const xScale = d3.scaleLinear()
        .range([0, innerWidth])
        .domain([-135, 135]);
    
    const yScale = d3.scaleLinear()
        .domain([-135, 135])
        .range([innerHeight, 0])
        .nice();
    
    
    const gScatterPlot = scatterPlot
        .append("g")
        .attr("transform", `translate(${margin.left}, ${margin.top})`);

    
    const xAxisG = gScatterPlot
        .append("g")
        .attr("class", "forColoring")
        .attr("id", "x-axis")
        .attr("transform", `translate(0, ${innerHeight})`);
    
   
    const yAxisG = gScatterPlot
        .append("g")
        .attr("class", "forColoring")
        .attr("id", "y-axis")
    
    
    xAxisG
        .append("text")
        .attr("class", "xAxisLabel")
        .attr("x", innerWidth / 2)
        .attr("y", 45);
    yAxisG
        .append("text")
        .attr("class", "yAxisLabel")
        .attr("x", -innerHeight / 2)
        .attr("y", -45)
        .style("transform", "rotate(-90deg)");
    gScatterPlot
        .append("text")
        .attr("id", "title")
        .attr("x", innerWidth / 2)
        .attr("y", -30);
    
    var radSize = 1;
    
    var colorScale = d3.scaleLinear()
        .domain([0, 1])
        .range(["#d3d3d3", "#ff0000"])
    

    if (color == "False"){
        gScatterPlot
        .selectAll("dot")
        .data(data)
        .enter()
        .append("circle")
        .attr("id", function(d,i) { return "circle-" + d.id; })
        .attr("class", "dot")
        .attr("cx", function (d) {
            return xScale(parseFloat(d.x));
        })
        .attr("cy", (d) => yScale(parseFloat(d.y)))
        .attr("r", (d) => d.size)
        .attr("stroke", (d) => d.stroke)
        .attr("fill", (d) => d.color)
        .attr("data-xvalue", (d) => parseFloat(d.x))
        .attr("data-yvalue", (d) => parseFloat(d.y))
        .on("click", function(d) {
            window.open(d.url)
        })
    } else {
        gScatterPlot
        .selectAll("dot")
        .data(data)
        .enter()
        .append("circle")
        .attr("id", function(d,i) { return "circle-" + d.id; })
        .attr("class", "dot")
        .attr("cx", function (d) {
            return xScale(parseFloat(d.x));
        })
        .attr("cy", (d) => yScale(parseFloat(d.y)))
        .attr("r", (d) => d.size)
        .attr("score", function(d){
            return colorScale(d.score);
        })
        .attr("fill", function(d){
            return colorScale(d.score);
        })
        //.attr("stroke", "black")
        //.attr("fill", "black")
        .attr("data-xvalue", (d) => parseFloat(d.x))
        .attr("data-yvalue", (d) => parseFloat(d.y))
        .on("click", function(d) {
            window.open(d.url)
        })

    }

    gScatterPlot
        .selectAll("circle")
        .each(function(d){
            if (d.size>8){
                d3.select(this).raise()
            }
        })

};


d3.json(
    //'http://127.0.0.1:5001/data.json'
    'http://127.0.0.1:5000/static/data/data.json'
  ).then((data) => {
    renderScatterPlot(data);
  });