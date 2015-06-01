function out = CPjustify(im)
% $Revision: 5969 $
    if min(im(:)) == max(im(:))
        out = zeros(size(im));
    else
        out = im - min(im(:));
        out = out / max(out(:));
    end