var scales = document.getElementById('iadscaleid').value;
var tasks = document.getElementById('iadtasksid');
if (tasks != null) {
    scales += ',' + tasks.value;
}
return scales;
