const buttonstring = "
<div class='accordion-item null panel svelte-g6tm5k'>
    <h2 class='accordion-header' id='h1703477325693-198'>
        <button class='accordion-button svelte-g6tm5k collapsed' type='button' data-bs-toggle='collapse' data-bs-target='#c1703477325693-198' aria-expanded='false' aria-controls='c1703477325693-198'>
            <div class='d-flex gap-2 align-items-center me-1'><i class='fa-solid fa-hippo' slot='icon'></i>Ayesha</div>
        </button>
    </h2>
    <div id='c1703477325693-198' class='accordion-collapse collapse' aria-labelledby='h1703477325693-198' data-bs-parent='#controls' style>
        <div class='accordion-body'>
            <div class='d-flex gap-4 align-items-center'>
            <fieldset class='mb-3'>
                <legend class='fs-5'>Quality</legend>
                <select id='iadscaleid' class='form-select' style='border:0;'>
                    <option value='size=full'>full size</option>
                </select> 
            </fieldset>
            <p class='mb-3'>
                <button type='button' class='btn btn-outline-dark' onclick='job.begin()'>
                    <div>
                        <span class='fa-solid fa-arrow-down'></span>
                        <span style='display:block' id='iadprogressid'></span>
                    </div>
                </button>
            </p>
            </div>
        </div>
    </div>
</div>
";
const ac = document.getElementById('controls')?.getElementsByClassName('border-top');
if (ac.length == 0) return false;
ac[0].insertAdjacentHTML('afterend', buttonstring);
return true;
