const buttonstring = "
<div class='topinblock button scale-btn'>
    <select id='iadscaleid' class='iadselect'>
        <option value='1' selected>★★★★</option>
    </select>
    <div class='iadlabel'>Quality</div>
</div>
<div class='topinblock download-btn'>
    <button class='button' type='button' onclick='job.begin();'>
        <div>
            <span class='iconochive-download'></span>
            <span class='icon-label' id='iadprogressid'></span>
        </div>
    </button>
</div>
";
const iadlabel = document.getElementsByClassName('iadlabel');
if (iadlabel.length > 0) return true;
const ab = document.getElementsByClassName('action-buttons-section');
if (ab.length == 0) return false;
ab[0].insertAdjacentHTML('afterbegin', buttonstring);
return true;
